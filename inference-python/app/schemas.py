from pydantic import BaseModel, Field, validator
import json

class LoanApplicationSchema(BaseModel):
    id: str
    applicantId: str
    amount: float = Field(..., gt=0, description="Loan amount must be strictly positive")
    termMonths: int = Field(..., gt=0, description="Term must be strictly positive")
    status: str
    
    @validator('amount')
    def amount_must_be_reasonable(cls, v):
        if v > 10000000:
            raise ValueError("Amount exceeds maximum allowed limit")
        return v

    @classmethod
    def from_outbox_json(cls, payload_str: str):
        data = json.loads(payload_str)
        return cls(**data)
