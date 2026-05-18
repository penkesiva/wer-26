# Idea 2 — Mark Car Parking Spot

```mermaid
---
config:
  flowchart:
    htmlLabels: true
    padding: 6
    nodeSpacing: 18
    rankSpacing: 30
---
flowchart LR
 subgraph SSGC["<b><span style=font-size:14px>SS-Glasses-Core</span></b>"]
    direction TB
        PolicyCP["<b>EventPolicy:Park</b><br>Gates · timing · negatives"]
        OrchCP["<b>EventOrchestrator:Park</b><br>Audio cue · capture · VLM · save"]
  end
 subgraph Mobile["<b><span style=font-size:16px>Mobile phone</span></b>"]
    direction TB
        VA["<b>Voice assistant</b><br>Hotword · GPS nudge · recall"]
        Sens["<b>Sensors</b><br>Motion · GPS"]
        SSGC
        VLM["<b>On-device VLM</b><br>Scene + OCR"]
        AMEM[("<b>Ambient-Memory</b><br>Frames + text")]
  end
 subgraph Glasses["<b><span style=font-size:16px>Glasses</span></b>"]
    direction TB
        Cam["<b>Camera</b>"]
        Mic["<b>Mic</b>"]
  end
 subgraph GCol[" "]
    direction TB
        User(["User"])
        Glasses
  end
 subgraph LegendRow["Legend"]
    direction LR
        LgG("<b>Glasses</b>")
        LgM("<b>Voice</b>")
        LgSen("<b>Sensors</b>")
        LgL("<b>VLM</b>")
        LgA("<b>Ambient</b>")
        LgS("<b>SS-Core</b>")
  end
 subgraph BottomDeck[" "]
    direction RL
        LegendRow
  end
    LgG --- LgM
    LgM --- LgSen
    LgSen --- LgL
    LgL --- LgA
    LgA --- LgS
    Mobile ~~~ GCol
    GCol ~~~ BottomDeck
    Sens -- ① context --> OrchCP
    OrchCP <-- ② policy --> PolicyCP
    OrchCP -- ③ audio cue --> Mic
    OrchCP -- ④ capture control --> Cam
    Cam -- ⑤ frames --> OrchCP
    OrchCP -- ⑥ analyze --> VLM
    VLM -- ⑦ text --> OrchCP
    OrchCP -- ⑧ save --> AMEM
    User --> Mic
    Mic -. speech .-> VA
    VA -- audio response --> Mic
    VA <-- smart fetch --> AMEM

     PolicyCP:::ssCoreLight
     OrchCP:::ssCoreLight
     VA:::mobile
     Sens:::sensors
     VLM:::onDeviceVlm
     AMEM:::ambientMem
     Cam:::glasses
     Mic:::glasses
     LgG:::legendGlasses
     LgM:::legendMobile
     LgSen:::legendSensors
     LgL:::legendVlm
     LgA:::legendAmbient
     LgS:::legendSS
     GCol:::colHidden
    classDef glasses fill:#9fd4ff,stroke:#0d47a1,color:#111,stroke-width:2px
    classDef mobile fill:#ffe0b2,stroke:#e65100,color:#111,stroke-width:2px
    classDef sensors fill:#fff59d,stroke:#f9a825,color:#111,stroke-width:2px
    classDef onDeviceVlm fill:#c8e6c9,stroke:#2e7d32,color:#111,stroke-width:2px
    classDef ambientMem fill:#ffccbc,stroke:#d84315,color:#111,stroke-width:2px
    classDef ssCoreLight fill:#ede7f6,stroke:#7e57c2,color:#111,stroke-width:2px
    classDef legendGlasses fill:#9fd4ff,stroke:#0d47a1,color:#111
    classDef legendMobile fill:#ffe0b2,stroke:#e65100,color:#111
    classDef legendSensors fill:#fff59d,stroke:#f9a825,color:#111
    classDef legendVlm fill:#c8e6c9,stroke:#2e7d32,color:#111
    classDef legendAmbient fill:#ffccbc,stroke:#d84315,color:#111
    classDef legendSS fill:#ede7f6,stroke:#7e57c2,color:#111
    classDef colHidden fill:none,stroke:none,color:#111
    style SSGC fill:#ede7f6,stroke:#7e57c2,stroke-width:2px
    style Glasses stroke:#0d47a1,stroke-width:2px
    style LegendRow fill:#fafafa,stroke:#bdbdbd,color:#111,stroke-width:1px
    style Mobile stroke:#e65100,stroke-width:2px
    style GCol fill:none,stroke:none
    style BottomDeck fill:none,stroke:none```
