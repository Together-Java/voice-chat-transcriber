# voice-chat-transcriber
Automatically joins voice channels and transcribes speech in real time using local, offline speech recognition.
Transcriptions are posted as embeds in a `#logs-vc` channel with per-user attribution (name and avatar).
Supports running multiple bot instances in parallel to cover more than one voice channel at a time.

## How It Works
1. A user joins a voice channel.
2. An available bot claims the channel and joins it.
3. Each user's audio is captured separately and streamed through Vosk for recognition.
4. Completed transcriptions are posted to `#logs-vc` as an embed attributed to that user.
5. When the channel is empty, the bot leaves and frees itself up for other channels.

Logs are only stored for a week on Discord and then automatically cleaned up.

## Vosk Model
The bot requires a Vosk speech recognition model. Two recommended options:

| Model | Size | Word Error Rate | Notes |
|---|---|---|---|
| `vosk-model-small-en-us-0.15` | ~40 MB | ~9.85% | Fast, low resource usage |
| `vosk-model-en-us-0.22` | ~1.8 GB | ~5.69% | Higher accuracy, recommended |

Download from [alphacephei.com/vosk/models](https://alphacephei.com/vosk/models) and extract to a `model/` directory in the project root.
