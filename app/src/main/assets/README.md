Place your trained TensorFlow Lite model here as:

`golf_audio_classifier.tflite`

Expected output labels (in order):
1. golf_hit
2. non_hit
3. speech
4. club_rattle
5. wind
6. cart_noise
7. footsteps
8. background

Until a model is added, the app uses `FakeAudioClassifier` for development and testing.
