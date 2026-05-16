import fitz  # PyMuPDF


class EmptyPdfText(ValueError):
    """The PDF has no extractable text layer (scanned image / empty)."""


def extract_text(data: bytes) -> str:
    """Extract the text layer from PDF bytes, fully in-memory.

    Never writes to disk. The caller must not log the return value.
    """
    text_parts: list[str] = []
    doc = fitz.open(stream=data, filetype="pdf")
    try:
        for page in doc:
            text_parts.append(page.get_text("text"))
    finally:
        doc.close()
    text = "\n".join(text_parts).strip()
    if not text:
        raise EmptyPdfText("PDF has no extractable text layer")
    return text
