from app.models.chat import RetrievedChunk
from app.rag.embeddings import embed
from app.rag.vectorstore import get_collection


def retrieve(query: str, top_k: int = 3) -> list[RetrievedChunk]:
    collection = get_collection()
    result = collection.query(query_embeddings=embed([query]), n_results=top_k)

    if not result["ids"][0]:
        return []

    return [
        RetrievedChunk(
            source=meta["source"],
            text=doc,
            # Chroma returns cosine distance; convert to a similarity score.
            score=1 - dist,
        )
        for doc, meta, dist in zip(
            result["documents"][0], result["metadatas"][0], result["distances"][0]
        )
    ]
