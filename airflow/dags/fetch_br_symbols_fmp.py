"""
Daily DAG: Fetch active stock symbols for Euronext Brussels from Financial Modeling Prep (FMP)
and store them in the investment_tracker.exchange_symbols table.
"""
from __future__ import annotations

import os
import requests
from datetime import datetime, timezone, timedelta
from urllib.parse import urlparse

import psycopg2
from psycopg2.extras import execute_values
from airflow import DAG
from airflow.operators.python import PythonOperator

DAG_ID = "fetch_brussels_symbols_fmp"
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

def fetch_and_store_symbols(**context) -> None:
    context["ti"].log.info("Fetching symbol list from Twelve Data (Euronext Brussels)...")
    
    # Twelve Data's reference endpoint is completely free and requires NO API KEY
    url = "https://api.twelvedata.com/stocks?exchange=XBRU"
    
    try:
        response = requests.get(url)
        response.raise_for_status()
        response_json = response.json()
        
        # Twelve Data nests the list inside a "data" key
        exchange_data = response_json.get("data", [])
    except Exception as e:
        context["ti"].log.error("Failed to fetch symbol list: %s", e)
        raise e

    now = datetime.now(timezone.utc)
    
    records_to_insert = []
    bond_filter_keywords = ["Bond", "Note", "Government","Kingdom", "Certificate", "OLO"]
    
    # Loop through the data and format it for Yahoo Finance
    for item in exchange_data:
        # Filter for actual stocks to avoid weird derivatives or funds
        if item.get("type") == "Common Stock":
            base_symbol = item.get("symbol")
            company_name = item.get("name", "Unknown")
            asset_type = 'bond' if any(keyword in company_name.lower() for keyword in bond_filter_keywords) else 'stock'
            # CRITICAL: Append .BR so Yahoo Finance can read it tomorrow
            yf_symbol = f"{base_symbol}.BR"
                
            records_to_insert.append((
                yf_symbol,
                company_name,
                "BR",
                now,
                asset_type
            ))

    if not records_to_insert:
        context["ti"].log.warning("No Brussels stock symbols found.")
        return

    # Bulk Insert into Database
    conn_params = _parse_db_url(get_investment_db_url())
    conn = psycopg2.connect(**conn_params)
    
    insert_query = """
        INSERT INTO exchange_symbols (symbol, company_name, exchange, last_updated, asset_type)
        VALUES %s
        ON CONFLICT (symbol) DO UPDATE SET 
            company_name = EXCLUDED.company_name,
            last_updated = EXCLUDED.last_updated,
            asset_type = EXCLUDED.asset_type;
    """

    try:
        with conn.cursor() as cur:
            execute_values(cur, insert_query, records_to_insert, page_size=1000)
        conn.commit()
        context["ti"].log.info("Successfully formatted and updated %d Brussels symbols.", len(records_to_insert))
    except Exception as e:
        conn.rollback()
        context["ti"].log.error("Database insertion failed: %s", e)
        raise e
    finally:
        conn.close()

with DAG(
    DAG_ID,
    default_args=DEFAULT_ARGS,
    description="Fetch Brussels symbols from FMP and update local DB",
    schedule="0 5 * * *",  # Runs daily at 05:00 UTC (1 hour before the price fetch)
    start_date=datetime(2025, 1, 1, tzinfo=timezone.utc),
    catchup=False,
    tags=["belgian", "symbols", "fmp", "setup"],
) as dag:
    PythonOperator(
        task_id="fetch_and_store_symbols",
        python_callable=fetch_and_store_symbols,
    )