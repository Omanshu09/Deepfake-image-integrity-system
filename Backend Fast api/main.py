import io
import numpy as np
import tensorflow as tf

from fastapi import FastAPI, UploadFile, File
from fastapi.middleware.cors import CORSMiddleware
from PIL import Image

app = FastAPI()

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Load model
model = tf.keras.models.load_model("deepfake_efficientnet_fine_tuned_v1.keras")

def prepare_image(image_bytes):
    img = Image.open(io.BytesIO(image_bytes))

    if img.mode != "RGB":
        img = img.convert("RGB")

    img = img.resize((224, 224))

    img_array = tf.keras.utils.img_to_array(img)

    img_array = np.expand_dims(img_array, axis=0)

    return img_array / 255.0

@app.get("/")
def home():
    return {"status": "Backend running"}

@app.post("/predict")
async def run_prediction(file: UploadFile = File(...)):
    contents = await file.read()

    ready_image = prepare_image(contents)

    prediction = model.predict(ready_image)

    if prediction.shape[-1] == 1:
        confidence = float(prediction[0][0])
        result = 1 if confidence > 0.5 else 0
    else:
        result = int(np.argmax(prediction, axis=1)[0])
        confidence = float(np.max(prediction))

    return {
        "prediction": result,
        "confidence": confidence,
        "status": "success"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="127.0.0.1", port=8000)