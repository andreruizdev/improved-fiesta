from typing import TypedDict, Annotated
import operator

class State(TypedDict):
    metrics: dict
    policy_report: str
    is_compliant: bool

def fetch_metrics(state: State) -> State:
    state['metrics'] = {"demographic_parity": 0.85, "error_rate": 0.01}
    return state

def policy_decision_point(state: State) -> State:
    metrics = state['metrics']
    
    if metrics.get("demographic_parity", 0) > 0.8:
        state['is_compliant'] = True
        state['policy_report'] = "Compliant with Fair lending act (DPR > 0.8)"
    else:
        state['is_compliant'] = False
        state['policy_report'] = "VIOLATION: Demographic parity below 0.8 threshold."
    
    return state

def generate_audit_report(state: State) -> State:
    report = f"AUDIT REPORT: {state['policy_report']}\nMetrics: {state['metrics']}"
    print(report)
    return state
