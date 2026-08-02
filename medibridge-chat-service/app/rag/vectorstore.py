from functools import lru_cache

import chromadb

from app.config import settings

COLLECTION_NAME = "app_faq"


@lru_cache
def get_client() -> chromadb.ClientAPI:
    return chromadb.PersistentClient(path=settings.chroma_persist_dir)


def get_collection():
    return get_client().get_or_create_collection(COLLECTION_NAME)
