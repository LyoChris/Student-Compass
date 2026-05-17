from fastapi import (
    APIRouter,
    Depends,
    HTTPException,
    UploadFile,
    status,
)

from app.core.ollama_client import OllamaUnavailable, get_ollama_client
from app.core.security import require_internal_secret
from app.modules.bank.pdf import EmptyPdfText, extract_text
from app.modules.bank.schemas import BankAnalyzeResponse
from app.modules.bank.service import analyze_text

router = APIRouter(
    prefix="/api/v1", dependencies=[Depends(require_internal_secret)]
)


@router.post("/bank/analyze", response_model=BankAnalyzeResponse)
async def bank_analyze(
    file: UploadFile,
    ollama=Depends(get_ollama_client),
) -> BankAnalyzeResponse:
    data = await file.read()  # bytes stay in memory only
    try:
        text = extract_text(data)
    except EmptyPdfText:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="PDF has no extractable text layer",
        )
    try:
        return await analyze_text(text, ollama)
    except OllamaUnavailable:
        raise HTTPException(
            status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
            detail="Analysis model unavailable",
        )
