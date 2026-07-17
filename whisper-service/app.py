import os
import base64
import io
from flask import Flask, request, jsonify
from flask_cors import CORS
from faster_whisper import WhisperModel

os.environ['HF_HUB_OFFLINE'] = '0'
os.environ['HF_ENDPOINT'] = 'https://hf-mirror.com'

app = Flask(__name__)
CORS(app)

MODEL_SIZE = os.environ.get("MODEL_SIZE", "tiny")
MODEL_PATH = os.environ.get("MODEL_PATH", "/models")

print(f"Loading Whisper model: {MODEL_SIZE}, path: {MODEL_PATH}")
try:
    model = WhisperModel(MODEL_SIZE, device="cpu", download_root=MODEL_PATH)
    print("Whisper model loaded successfully")
except Exception as e:
    print(f"Warning: Failed to load model: {e}")
    model = None

def get_model():
    global model
    if model is None:
        print("Loading model...")
        model = WhisperModel(MODEL_SIZE, device="cpu", download_root=MODEL_PATH)
    return model

@app.route('/health', methods=['GET'])
def health():
    return jsonify({"status": "ok", "model_loaded": model is not None})

@app.route('/transcribe', methods=['POST'])
def transcribe():
    try:
        data = request.get_json()
        
        if not data:
            return jsonify({"error": "No data provided"}), 400
        
        audio_data = data.get('audio')
        language = data.get('language')
        
        if not audio_data:
            return jsonify({"error": "No audio data provided"}), 400
        
        try:
            audio_bytes = base64.b64decode(audio_data)
        except Exception as e:
            return jsonify({"error": f"Invalid base64 audio data: {str(e)}"}), 400
        
        audio_file = io.BytesIO(audio_bytes)
        
        current_model = get_model()
        transcribe_kw = {"beam_size": 5}
        if language and str(language).strip().lower() not in ("auto", ""):
            transcribe_kw["language"] = str(language).strip()
        segments, info = current_model.transcribe(audio_file, **transcribe_kw)
        
        results = []
        for segment in segments:
            results.append({
                "text": segment.text,
                "start": segment.start,
                "end": segment.end
            })
        
        full_text = " ".join([r["text"] for r in results])
        
        return jsonify({
            "text": full_text,
            "segments": results,
            "language": info.language,
            "language_probability": info.language_probability
        })
        
    except Exception as e:
        print(f"Transcription error: {str(e)}")
        return jsonify({"error": str(e)}), 500

@app.route('/transcribe/file', methods=['POST'])
def transcribe_file():
    try:
        if 'file' not in request.files:
            return jsonify({"error": "No file provided"}), 400
        
        file = request.files['file']
        language = request.form.get('language', 'zh')
        
        current_model = get_model()
        segments, info = current_model.transcribe(
            file.stream,
            language=language,
            beam_size=5
        )
        
        results = []
        for segment in segments:
            results.append({
                "text": segment.text,
                "start": segment.start,
                "end": segment.end
            })
        
        full_text = " ".join([r["text"] for r in results])
        
        return jsonify({
            "text": full_text,
            "segments": results,
            "language": info.language,
            "language_probability": info.language_probability
        })
        
    except Exception as e:
        print(f"Transcription error: {str(e)}")
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
