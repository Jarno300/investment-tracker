"""
Daily DAG: cache Brussels stock metadata and quotes from Postgres into Redis.
Runs after the fetch_belgian_stocks DAG task has finished successfully.
"""
from __future__ import annotations

import json
import os
from datetime import datetime, timedelta, timezone
from urllib.parse import urlparse

import psycopg2
import redis
from airflow import DAG
from airflow.operators.python import PythonOperator
from airflow.sensors.external_task import ExternalTaskSensor

DAG_ID = "cache_br_stocks_to_redis"
DEFAULT_ARGS = {
    "owner": "airflow",
    "retries": 1,
    "retry_delay": timedelta(minutes=5),
}

REDIS_QUOTES_KEY = "br:stocks:quotes"
REDIS_METADATA_KEY = "br:stocks:metadata"
REDIS_LAST_REFRESH_KEY = "br:stocks:last_refresh"
REDIS_TTL_SECONDS = 48 * 60 * 60


def get_investment_db_url() -> str:
    url = os.environ.get("INVESTMENT_DB_URL")
    if not url:
        raise RuntimeError("INVESTMENT_DB_URL environment variable is not set")
    return url


def get_redis_url() -> str:
    url = os.environ.get("REDIS_URL")
    if not url:
        raise RuntimeError("REDIS_URL environment variable is not set")
    return url


def _parse_db_url(url: str) -> dict:
    parsed = urlparse(url)
    return {
        "host": parsed.hostname,
        "port": parsed.port or 5432,
        "dbname": parsed.path.lstrip("/"),
        "user": parsed.username,
        "password": parsed.password,
    }


def cache_br_stock_data_to_redis(**context) -> None:
    conn_params = _parse_db_url(get_investment_db_url())
    conn = psycopg2.connect(**conn_params)

    sql = """
        SELECT
            es.symbol,
            es.company_name,
            sqc.price,
            sqc.previous_close,
            sqc.change_amount,
            sqc.change_percent,
            sqc.last_fetched_at
        FROM exchange_symbols es
        LEFT JOIN stock_quote_cache sqc
          ON UPPER(es.symbol) = UPPER(sqc.symbol)
        WHERE es.exchange IN ('BR', 'Brussels')
    """

    try:
        with conn.cursor() as cur:
            cur.execute(sql)
            rows = cur.fetchall()
    finally:
        conn.close()

    if not rows:
        context["ti"].log.warning("No Brussels symbols found in exchange_symbols.")
        return

    redis_client = redis.Redis.from_url(get_redis_url(), decode_responses=True)

    quote_payload = {}
    metadata_payload = {}

    for row in rows:
        (
            symbol,
            company_name,
            price,
            previous_close,
            change_amount,
            change_percent,
            last_fetched_at,
        ) = row

        if not symbol:
            continue

        normalized_symbol = symbol.strip().upper()
        metadata_payload[normalized_symbol] = json.dumps(
            {
                "symbol": normalized_symbol,
                "name": company_name or normalized_symbol,
                "region": "Brussels",
                "currency": "EUR",
            }
        )

        if price is None:
            continue

        quote_payload[normalized_symbol] = json.dumps(
            {
                "symbol": normalized_symbol,
                "price": float(price),
                "previousClose": float(previous_close) if previous_close is not None else None,
                "change": float(change_amount) if change_amount is not None else None,
                "changePercent": change_percent,
                "lastFetchedAt": last_fetched_at.isoformat() if last_fetched_at else None,
            }
        )

    if not metadata_payload:
        context["ti"].log.warning("No valid metadata rows were prepared for Redis.")
        return

    now = datetime.now(timezone.utc).isoformat()
    with redis_client.pipeline() as pipe:
        pipe.delete(REDIS_QUOTES_KEY, REDIS_METADATA_KEY)
        pipe.hset(REDIS_METADATA_KEY, mapping=metadata_payload)
        if quote_payload:
            pipe.hset(REDIS_QUOTES_KEY, mapping=quote_payload)
        pipe.set(REDIS_LAST_REFRESH_KEY, now)
        pipe.expire(REDIS_METADATA_KEY, REDIS_TTL_SECONDS)
        pipe.expire(REDIS_QUOTES_KEY, REDIS_TTL_SECONDS)
        pipe.expire(REDIS_LAST_REFRESH_KEY, REDIS_TTL_SECONDS)
        pipe.execute()

    context["ti"].log.info(
        "Cached %d symbols and %d quote rows in Redis",
        len(metadata_payload),
        len(quote_payload),
    )


with DAG(
    DAG_ID,
    default_args=DEFAULT_ARGS,
    description="Populate Redis with Brussels stock metadata and quotes",
    schedule="0 6 * * *",
    start_date=datetime(2025, 1, 1, tzinfo=timezone.utc),
    catchup=False,
    tags=["belgian", "stocks", "redis", "cache"],
) as dag:
    wait_for_stock_fetch = ExternalTaskSensor(
        task_id="wait_for_fetch_belgian_stocks",
        external_dag_id="fetch_belgian_stocks",
        external_task_id="fetch_and_cache",
        allowed_states=["success"],
        failed_states=["failed", "skipped"],
        mode="reschedule",
        poke_interval=30,
        timeout=60 * 60,
    )

    cache_to_redis = PythonOperator(
        task_id="cache_to_redis",
        python_callable=cache_br_stock_data_to_redis,
    )

    wait_for_stock_fetch >> cache_to_redis
