// Idea 2 — Mark Car Parking Spot
// No OEM SDK — camera frames → SS-Glasses-Core → EventOrchestrator:Park
// Paste into https://app.eraser.io (Diagram-as-Code)

User [icon: user, color: yellow]

Glasses [icon: glasses, color: orange] {
  Camera [icon: camera, label: "Camera\npolicy-shaped capture"]
  Mic_Display [icon: mic, label: "Mic + speakers + display"]
}

Mobile [icon: smartphone, color: blue] {

  Sensors_Block [icon: activity, color: red, label: "Sensors (on-device)"] {
    Motion_Activity [icon: car, label: "Motion Activity API"]
    GPS [icon: map-pin, label: "GPS"]
  }

  SS_Glasses_Core [icon: cog, label: "SS-Glasses-Core"] {
    Event_Policy_Park [icon: shield, label: "EventPolicy:Park\ngates · timing · negatives"]
    Event_Orchestrator_Park [icon: route, label: "EventOrchestrator:Park\ncue · capture · VLM · save"]
  }

  Voice_Assistant [icon: message-circle, color: teal, label: "On-device voice assistant\nhotword · GPS nudge · recall"]

  OnDevice_VLM [icon: cpu, color: green, label: "On-device VLM\noutside SS-Glasses-Core"]

  Ambient_Memory [icon: database, color: brown, label: "Ambient-Memory\nframes + text"]
}

// Park path
Motion_Activity > Event_Orchestrator_Park: activity context
GPS > Event_Orchestrator_Park: speed + position
Event_Orchestrator_Park > Event_Policy_Park: consult
Event_Policy_Park > Event_Orchestrator_Park: allow · defer · extend · veto

Camera > Event_Orchestrator_Park: image frames into SS-Glasses-Core
Event_Orchestrator_Park > Camera: capture control
Event_Orchestrator_Park > Mic_Display: TTS / cue

Event_Orchestrator_Park > OnDevice_VLM: image batch
OnDevice_VLM > Event_Orchestrator_Park: text + OCR JSON

Event_Orchestrator_Park > Ambient_Memory: write marker

// Recall
User > Mic_Display: speech
Mic_Display > Voice_Assistant: hotword / NL
GPS > Voice_Assistant: nearness nudge / smart fetch
Voice_Assistant > Ambient_Memory: read marker
Voice_Assistant > Mic_Display: TTS + thumbnails
