# Idea 2 — Mark Car Parking Spot

```mermaid
%% Idea 2 — Mark Car Parking Spot · mermaid.live
%% flowchart LR: Mobile (left) | Glasses (right) | Legend (bottom)
%% <b>…</b> = bold title inside a box
%%{init: {'flowchart': {'htmlLabels': true, 'padding': 10, 'nodeSpacing': 30, 'rankSpacing': 50}}}%%

flowchart LR
    subgraph Mobile["<b><span style='font-size:22px'>Mobile phone</span></b>"]
        direction TB
        VA["<b>Voice assistant</b><br/>Hotword · GPS nudge · recall"]
        Sens["<b>Sensors</b><br/>Motion · GPS"]
        subgraph SSGC["<b><span style='font-size:18px'>SS-Glasses-Core</span></b>"]
            direction TB
            PolicyCP["<b>EventPolicy:Park</b><br/>Gates · timing · negatives"]
            OrchCP["<b>EventOrchestrator:Park</b><br/>Audio/UI cue · capture · on-device VLM · save"]
        end
        VLM["<b>On-device VLM</b><br/>Scene + OCR"]
        AMEM[("<b>Ambient-Memory</b><br/>Frames + text")]
    end

    subgraph GCol[" "]
        direction TB
        User([User])
        subgraph Glasses["<b><span style='font-size:22px'>Glasses</span></b>"]
            direction LR
            Cam["<b>Camera</b><br/>Policy-shaped<br/>capture"]
            Mic["<b>Mic &amp; display</b>"]
        end
    end

    subgraph BottomDeck[" "]
        direction RL
        subgraph LegendRow["Legend"]
            direction LR
            LgG("<b>Glasses</b>")
            LgM("<b>Voice</b>")
            LgSen("<b>Sensors</b>")
            LgL("<b>VLM</b>")
            LgA("<b>Ambient</b>")
            LgS("<b>SS-Core</b>")
            LgG --- LgM --- LgSen --- LgL --- LgA --- LgS
        end
    end

    Mobile ~~~ GCol
    GCol ~~~ BottomDeck

    Sens -->|"① context"| OrchCP
    OrchCP <-->|"② policy"| PolicyCP
    OrchCP -->|"③ audio/UI cue"| Mic
    OrchCP -->|"④ capture control"| Cam
    Cam -->|"⑤ frames"| OrchCP
    OrchCP -->|"⑥ analyze"| VLM
    VLM -->|"⑦ text"| OrchCP
    OrchCP -->|"⑧ save"| AMEM

    User --> Mic
    Mic -.->|speech| VA
    VA -->|TTS · UI| Mic
    VA <-->|smart fetch| AMEM

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
    style GCol fill:none,stroke:none
    style Glasses stroke:#0d47a1,stroke-width:3px,padding:20px
    style Mobile stroke:#e65100,stroke-width:2px
    style BottomDeck fill:none,stroke:none
    style LegendRow fill:#fafafa,stroke:#bdbdbd,color:#111,stroke-width:1px

    class Cam,Mic glasses
    class Sens sensors
    class VA mobile
    class VLM onDeviceVlm
    class AMEM ambientMem
    class PolicyCP,OrchCP ssCoreLight
    class GCol colHidden

    class LgG legendGlasses
    class LgM legendMobile
    class LgSen legendSensors
    class LgL legendVlm
    class LgA legendAmbient
    class LgS legendSS
```
