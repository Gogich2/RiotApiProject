from sqlalchemy import create_engine

from config import get_database_url


engine = create_engine(get_database_url())