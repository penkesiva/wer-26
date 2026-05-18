# Idea 2 — Mark Car Parking Spot

```mermaid
%% Idea 2 — Mark Car Parking Spot · mermaid.live
%% Layout: Mobile (top) | Glasses (bottom) | Legend
%% <b>…</b> = bold title inside a box
%%{init: {'flowchart': {'htmlLabels': true, 'padding': 6, 'nodeSpacing': 20, 'rankSpacing': 28}}}%%

flowchart TB
    subgraph MainRow[" "]
        direction TB
        subgraph Mobile["<b><span style='font-size:20px'>Mobile phone</span></b>"]
            direction TB
            VA["<b>Voice assistant</b><br/>Hotword · GPS nudge · recall"]
            Sens["<b>Sensors</b><br/>Motion · GPS"]
            subgraph SSGC["<b><span style='font-size:16px'>SS-Glasses-Core</span></b>"]
                direction TB
                PolicyCP["<b>EventPolicy:Park</b><br/>Gates · timing · negatives"]
                OrchCP["<b>EventOrchestrator:Park</b><br/>Audio cue · capture · VLM · save"]
            end
            VLM["<b>On-device VLM</b><br/>Scene + OCR"]
            AMEM[("<b>Ambient-Memory</b><br/>Frames + text")]
        end

        subgraph GCol[" "]
            direction TB
            User([User])
            ParkAction["<b>Park & walk away</b><br/>GPS · motion change"]
            subgraph Glasses["<b><span style='font-size:20px'>Glasses</span></b>"]
                direction TB
                Cam["<b>Camera</b>"]
                Mic["<b>Mic</b>"]
            end
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

    MainRow ~~~ BottomDeck

    User --> ParkAction
    ParkAction --> Sens
    Sens -->|"① context"| OrchCP
    OrchCP <-->|"② policy"| PolicyCP
    OrchCP -->|"③ audio cue"| Glasses
    OrchCP -->|"④ capture control"| Cam
    Cam -.->|"⑤ frames"| OrchCP
    OrchCP -->|"⑥ analyze"| VLM
    VLM -->|"⑦ text"| OrchCP
    OrchCP -->|"⑧ save"| AMEM

    User --> Mic
    Mic -. speech .-> VA
    VA -->|audio response| Glasses
    VA <-->|smart fetch| AMEM

    classDef glasses fill:#cfe4f5,stroke:#3a6fa0,color:#111,stroke-width:2px
    classDef mobile fill:#fde8cc,stroke:#b86a2b,color:#111,stroke-width:2px
    classDef sensors fill:#fef5c7,stroke:#b8901f,color:#111,stroke-width:2px
    classDef onDeviceVlm fill:#d9ead3,stroke:#4e7a3a,color:#111,stroke-width:2px
    classDef ambientMem fill:#f4d4c4,stroke:#a85a35,color:#111,stroke-width:2px
    classDef ssCoreLight fill:#e6def0,stroke:#7e57c2,color:#111,stroke-width:2px
    classDef userGesture fill:#e1edf5,stroke:#4a7aa3,color:#111,stroke-width:2px
    classDef legendGlasses fill:#cfe4f5,stroke:#3a6fa0,color:#111
    classDef legendMobile fill:#fde8cc,stroke:#b86a2b,color:#111
    classDef legendSensors fill:#fef5c7,stroke:#b8901f,color:#111
    classDef legendVlm fill:#d9ead3,stroke:#4e7a3a,color:#111
    classDef legendAmbient fill:#f4d4c4,stroke:#a85a35,color:#111
    classDef legendSS fill:#e6def0,stroke:#7e57c2,color:#111
    classDef colHidden fill:none,stroke:none,color:#111

    style SSGC fill:#e6def0,stroke:#7e57c2,stroke-width:2px
    style GCol fill:none,stroke:none
    style Glasses stroke:#3a6fa0,stroke-width:2px
    style Mobile stroke:#b86a2b,stroke-width:2px
    style MainRow fill:none,stroke:none
    style BottomDeck fill:none,stroke:none
    style LegendRow fill:#fafafa,stroke:#bdbdbd,color:#111,stroke-width:1px

    class Cam,Mic glasses
    class ParkAction userGesture
    class VA mobile
    class Sens sensors
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
