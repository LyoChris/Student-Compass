from pydantic import BaseModel


class BankInsights(BaseModel):
    totalSpent: float
    periodLabel: str


class CategoryAmount(BaseModel):
    category: str
    amount: float


class ClarifyingQuestion(BaseModel):
    prompt: str
    merchant: str
    amount: float
    isImpulsive: bool


class BankAnalyzeResponse(BaseModel):
    insights: BankInsights
    categories: list[CategoryAmount]
    questions: list[ClarifyingQuestion]
