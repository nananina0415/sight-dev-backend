from flask import Flask, request, jsonify

app = Flask(__name__)

API_KEY = "0000"

USERS = {
    "member":  {"userId": 1},
    "manager": {"userId": 2},
}


@app.route("/internal/auth", methods=["POST"])
def internal_auth():
    if request.headers.get("x-api-key") != API_KEY:
        return jsonify({"login": False}), 401
    return jsonify({"login": True, "userId": USERS["manager"]["userId"]})


@app.route("/internal/point", methods=["POST"])
def internal_point():
    return jsonify({}), 200


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=8080)
