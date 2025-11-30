from sqlalchemy.ext.asyncio import create_async_engine, async_sessionmaker, AsyncSession
from typing import AsyncGenerator
from config import DB_CONFIG

URL = (f"mysql+aiomysql://{DB_CONFIG['user']}:{DB_CONFIG['password']}@"
       f"{DB_CONFIG['host']}:{DB_CONFIG['port']}/{DB_CONFIG['database']}")

engine = create_async_engine(URL, pool_size=5, max_overflow=10, echo=False)
SessionLocal = async_sessionmaker(engine, expire_on_commit=False, class_=AsyncSession)

async def get_session() -> AsyncGenerator[AsyncSession, None]:
    async with SessionLocal() as session:
        yield session
