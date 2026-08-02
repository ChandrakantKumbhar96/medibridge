"""Chunk app_faq/*.md into the Chroma collection. Run manually:

    python -m app.rag.ingest
"""
from pathlib import Path

from app.rag.embeddings import embed
from app.rag.vectorstore import get_collection

FAQ_DIR = Path(__file__).resolve().parent.parent / "knowledge" / "app_faq"


def chunk_markdown(text: str, source: str) -> list[dict]:
    # Split on markdown headers - each section becomes one chunk. Good
    # enough for short FAQ docs; revisit if a doc grows past a few screens.
    sections = ["## " + s for s in text.split("## ") if s.strip()]
    return [
        {"id": f"{source}:{i}", "source": source, "text": section.strip()}
        for i, section in enumerate(sections)
    ]


def run() -> int:
    collection = get_collection()
    chunks: list[dict] = []
    for path in sorted(FAQ_DIR.glob("*.md")):
        chunks.extend(chunk_markdown(path.read_text(encoding="utf-8"), path.stem))

    if not chunks:
        print(f"No markdown files found in {FAQ_DIR}")
        return 0

    collection.upsert(
        ids=[c["id"] for c in chunks],
        embeddings=embed([c["text"] for c in chunks]),
        documents=[c["text"] for c in chunks],
        metadatas=[{"source": c["source"]} for c in chunks],
    )
    print(f"Ingested {len(chunks)} chunks from {FAQ_DIR}")
    return len(chunks)


if __name__ == "__main__":
    run()
