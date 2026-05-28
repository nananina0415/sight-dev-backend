from flask import Flask, request, jsonify, make_response
import secrets
import os

app = Flask(__name__)

API_KEY = os.environ.get("AUTH_SERVICE_API_KEY", "")

USERS = {
    "member":  {"password": "0000", "userId": 1},
    "manager": {"password": "0000", "userId": 2},
}

sessions: dict[str, int] = {}


@app.route("/login", methods=["POST"])
def login():
    data = request.get_json(silent=True) or {}
    user = USERS.get(data.get("username", ""))
    if not user or user["password"] != data.get("password", ""):
        return jsonify({"message": "인증 실패"}), 401
    token = secrets.token_hex(32)
    sessions[token] = user["userId"]
    resp = make_response(jsonify({"message": "ok"}), 200)
    resp.set_cookie("session", token, httponly=True, samesite="Lax")
    return resp


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
    if not token or token not in sessions:
        return jsonify({"login": False})
    return jsonify({"login": True, "userId": sessions[token]})


@app.route("/internal/point", methods=["POST"])
def internal_point():
    return jsonify({}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
