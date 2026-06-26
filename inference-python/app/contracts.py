from pydantic import BaseModel, Field, field_validator
import json

class TelemetryPayloadSchema(BaseModel):
    id: str = Field(..., description="Unique event generation UUID")
    deviceId: str = Field(..., description="Target machine edge hardware identifier")
    readingValue: float = Field(..., gt=0.0, description="Metric value must be explicitly positive")
    operatingHours: int = Field(..., ge=0)

    @field_validator('readingValue')
    @classmethod
    def structural_limit_check(cls, value: float) -> float:
        if value > 50000.0:
            raise ValueError("Telemetry metric reading exceeds physical safety parameters")
        return value

    @classmethod
    def parse_raw_outbox(cls, json_str: str):
        parsed = json.loads(json_str)
        return cls(**parsed)