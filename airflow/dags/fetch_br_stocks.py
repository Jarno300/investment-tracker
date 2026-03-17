"""
Daily DAG: Fetch Belgian stock quotes from Yahoo Finance dynamically
based on the symbols stored in the exchange_symbols table, and cache them
in the investment_tracker.stock_quote_cache table.
"""
from __future__ import annotations

import os
import time
from datetime import datetime, timedelta, timezone
from urllib.parse import urlparse

import psycopg2
from psycopg2.extras import execute_values
import yfinance as yf
from airflow import DAG
from airflow.operators.python import PythonOperator

DAG_ID = "fetch_belgian_stocks"
DEFAULT_ARGS = {
    "owner": "airflow",
    "retries": 2,
    "retry_delay": timedelta(minutes=5),
}

def get_investment_db_url() -> str:
    url = os.environ.get("INVESTMENT_DB_URL")
    if not url:
        raise RuntimeError("INVESTMENT_DB_URL environment variable is not set")
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

def fetch_and_cache_belgian_stocks(**context) -> None:
    conn_params = _parse_db_url(get_investment_db_url())
    conn = psycopg2.connect(**conn_params)
    
    # 1. Fetch the dynamic list of symbols from your database
    try:
        with conn.cursor() as cur:
            # Support both legacy "Brussels" and normalized "BR" exchange values.
            cur.execute(
                "SELECT symbol FROM exchange_symbols WHERE exchange IN ('Brussels', 'BR');"
            )
            # Flatten the list of tuples returned by fetchall()
            db_symbols = [row[0] for row in cur.fetchall()]
    except Exception as e:
        conn.close()
        context["ti"].log.error("Failed to fetch symbols from DB: %s", e)
        raise e

    if not db_symbols:
        context["ti"].log.warning("No symbols found in the exchange_symbols table. Exiting.")
        conn.close()
        return

    context["ti"].log.info("Fetched %d symbols from the database. Starting Yahoo Finance fetch...", len(db_symbols))

    # 2. Fetch prices from Yahoo Finance
    now = datetime.now(timezone.utc)
    records_to_insert = []

    for symbol in db_symbols:
        try:
            ticker = yf.Ticker(symbol)
            info = ticker.fast_info
            price = getattr(info, "last_price", None) or getattr(info, "previous_close", None)
            previous_close = getattr(info, "previous_close", None)
            
            if price is None:
                continue
            if previous_close is None:
                previous_close = price
                
            change_amount = (price - previous_close) if (price is not None and previous_close is not None) else 0.0
            change_percent = (
                f"{((price - previous_close) / previous_close * 100):.2f}%" if previous_close and previous_close != 0 else "0.00%"
            )
            
            # Append as a tuple for execute_values compatibility
            records_to_insert.append((
                symbol,
                float(price),
                float(previous_close),
                float(change_amount),
                change_percent,
                now
            ))
            
            # CRITICAL: Be kind to Yahoo's servers to prevent IP bans
            time.sleep(0.5) 
            
        except Exception as e:
            context["ti"].log.warning("Skip %s: %s", symbol, e)
            continue

    if not records_to_insert:
        context["ti"].log.warning("No quotes fetched for any symbol")
        conn.close()
        return

    # 3. Bulk Insert into the cache table
    insert_query = """
        INSERT INTO stock_quote_cache (symbol, price, previous_close, change_amount, change_percent, last_fetched_at)
        VALUES %s
        ON CONFLICT (symbol) DO UPDATE SET
            price = EXCLUDED.price,
            previous_close = EXCLUDED.previous_close,
            change_amount = EXCLUDED.change_amount,
            change_percent = EXCLUDED.change_percent,
            last_fetched_at = EXCLUDED.last_fetched_at;
    """

    try:
        with conn.cursor() as cur:
            execute_values(cur, insert_query, records_to_insert, page_size=1000)
        conn.commit()
        context["ti"].log.info("Successfully cached %d Belgian stock quotes", len(records_to_insert))
    except Exception as e:
        conn.rollback()
        context["ti"].log.error("Failed to insert quotes into DB: %s", e)
        raise e
    finally:
        conn.close()

with DAG(
    DAG_ID,
    default_args=DEFAULT_ARGS,
    description="Fetch Belgian stock quotes from Yahoo Finance dynamically and cache in app DB",
    schedule="0 6 * * *",  # Runs daily at 06:00 UTC (1 hour AFTER the FMP DAG runs)
    start_date=datetime(2025, 1, 1, tzinfo=timezone.utc),
    catchup=False,
    tags=["belgian", "stocks", "yahoo", "cache", "dynamic"],
) as dag:
    PythonOperator(
        task_id="fetch_and_cache",
        python_callable=fetch_and_cache_belgian_stocks,
    )