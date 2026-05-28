from flask import Flask, request, jsonify
import os

app = Flask(__name__)

API_KEY = os.environ.get("AUTH_SERVICE_API_KEY", "")

USERS = {
    "member":  {"userId": 1},
    "manager": {"userId": 2},
}


@app.route("/internal/auth", methods=["POST"])
def internal_auth():
    if request.headers.get("x-api-key") != API_KEY:
        return jsonify({"login": False}), 401
    token = None
    for part in request.headers.get("Cookie", "").split(";"):
        part = part.strip()
        if part.startswith("session="):
            token = part[len("session="):]
            break
    user = USERS.get(token) if token else None
    if not user:
        return jsonify({"login": False})
    return jsonify({"login": True, "userId": user["userId"]})


@app.route("/internal/point", methods=["POST"])
def internal_point():
    return jsonify({}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
