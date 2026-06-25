import xgboost as xgb
import numpy as np
import shap

class RiskModel:
    def __init__(self):
        self.model = xgb.XGBClassifier(n_estimators=10)
        X_dummy = np.array([[5000, 12], [20000, 36], [1000, 6], [100000, 60]])
        y_dummy = np.array([0, 1, 0, 1])
        self.model.fit(X_dummy, y_dummy)
        self.explainer = shap.Explainer(self.model)

    def predict(self, amount: float, term: int):
        features = np.array([[amount, term]])
        prob = self.model.predict_proba(features)[0][1]
        
        shap_values = self.explainer(features)
        shap_dict = {
            "amount_importance": float(shap_values.values[0][0]),
            "term_importance": float(shap_values.values[0][1])
        }
        
        return float(prob), shap_dict

model_instance = RiskModel()
