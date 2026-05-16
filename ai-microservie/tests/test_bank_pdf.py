import fitz
import pytest

from app.modules.bank.pdf import EmptyPdfText, extract_text


def _pdf_with_text(text: str) -> bytes:
    doc = fitz.open()
    page = doc.new_page()
    page.insert_text((72, 72), text)
    data = doc.tobytes()
    doc.close()
    return data


def _pdf_no_text() -> bytes:
    doc = fitz.open()
    doc.new_page()
    data = doc.tobytes()
    doc.close()
    return data


def test_extract_text_returns_layer():
    out = extract_text(_pdf_with_text("2026-03-01 LIDL -42.10"))
    assert "LIDL" in out


def test_empty_text_layer_raises():
    with pytest.raises(EmptyPdfText):
        extract_text(_pdf_no_text())
