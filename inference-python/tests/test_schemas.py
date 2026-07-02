import json
import pytest
from abc import ABC, abstractmethod
from pydantic import ValidationError

class BaseSchemaTestSuite(ABC):
    """
    Abstract base class to enforce a standard interface for schema testing.
    This decoupled approach ensures new schemas must implement tests for standard security
    and data validation scenarios.
    """

    @abstractmethod
    def test_valid_payload(self):
        """Must test a fully valid payload."""
        pass

    @abstractmethod
    def test_missing_required_fields(self):
        """Must test behavior when required fields are missing."""
        pass

    @abstractmethod
    def test_boundary_and_edge_cases(self):
        """Must test extreme values, type mismatches, or malicious inputs."""
        pass

    @abstractmethod
    def test_deserialization(self):
        """Must test JSON/string deserialization safely."""
        pass

from app.schemas import LoanApplicationSchema

class TestLoanApplicationSchema(BaseSchemaTestSuite):
    """
    Test suite for the LoanApplicationSchema.
    """

    @pytest.fixture
    def valid_data(self):
        return {
            "id": "loan_123",
            "applicantId": "app_456",
            "amount": 50000.0,
            "termMonths": 60,
            "status": "PENDING"
        }

    def test_valid_payload(self, valid_data):
        schema = LoanApplicationSchema(**valid_data)
        assert schema.id == valid_data["id"]
        assert schema.amount == valid_data["amount"]

    @pytest.mark.parametrize("missing_field", ["id", "applicantId", "amount", "termMonths", "status"])
    def test_missing_required_fields(self, valid_data, missing_field):
        invalid_data = valid_data.copy()
        del invalid_data[missing_field]
        with pytest.raises(ValidationError) as exc_info:
            LoanApplicationSchema(**invalid_data)

        errors = exc_info.value.errors()
        assert any(e["loc"][0] == missing_field for e in errors)
        assert any(e["type"] == "missing" for e in errors)

    @pytest.mark.parametrize("field, invalid_value, error_msg", [
        ("amount", 0, "Input should be greater than 0"),
        ("amount", -100, "Input should be greater than 0"),
        ("termMonths", 0, "Input should be greater than 0"),
        ("termMonths", -12, "Input should be greater than 0"),
        ("amount", 10000000.1, "Amount exceeds maximum allowed limit")
    ])
    def test_boundary_and_edge_cases(self, valid_data, field, invalid_value, error_msg):
        invalid_data = valid_data.copy()
        invalid_data[field] = invalid_value

        with pytest.raises(ValidationError) as exc_info:
            LoanApplicationSchema(**invalid_data)

        errors = exc_info.value.errors()
        assert any(e["loc"][0] == field for e in errors)
        assert any(error_msg in str(e["msg"]) for e in errors)

    def test_deserialization(self, valid_data):
        # Test valid JSON deserialization
        json_payload = json.dumps(valid_data)
        schema = LoanApplicationSchema.from_outbox_json(json_payload)
        assert schema.id == valid_data["id"]

        # Test invalid JSON deserialization (security/robustness)
        with pytest.raises(json.JSONDecodeError):
            LoanApplicationSchema.from_outbox_json("invalid JSON payload {")
