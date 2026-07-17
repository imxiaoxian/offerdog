FROM python:3.11-slim

WORKDIR /app

ENV HF_ENDPOINT=https://hf-mirror.com

RUN sed -i 's/deb.debian.org/mirrors.aliyun.com/g' /etc/apt/sources.list.d/debian.sources && \
    pip config set global.index-url https://pypi.tuna.tsinghua.edu.cn/simple && \
    pip config set global.trusted-host pypi.tuna.tsinghua.edu.cn

RUN apt-get update && apt-get install -y \
    ffmpeg \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN pip install --no-cache-dir --upgrade pip && \
    pip install --no-cache-dir faster-whisper flask flask-cors

RUN python -c "from faster_whisper import WhisperModel; WhisperModel('tiny', device='cpu', download_root='/models')"

COPY app.py .

EXPOSE 5000

CMD ["python", "app.py"]
