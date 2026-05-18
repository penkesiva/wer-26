# Idea 1 — Gesture-Driven Image Crop for Visual Q&A

```mermaid
%% Idea 1 — Gesture-Driven Image Crop for Visual Q&A · mermaid.live
%% Layout: User + Glasses (left) → Mobile (center) → Cloud (right)
%% <b>…</b> = bold title inside a box
%%{init: {'flowchart': {'htmlLabels': true, 'padding': 6, 'nodeSpacing': 20, 'rankSpacing': 28}}}%%

flowchart TB
    subgraph MainRow[" "]
        direction LR
        subgraph GCol[" "]
            direction TB
            User([User])
            TwoHand["<b>Two-handed close gesture</b><br/>1–2 s visibility"]
            subgraph Glasses["<b><span style='font-size:20px'>Glasses</span></b>"]
                direction TB
                Cam["<b>Camera</b><br/>1 FPS frame buffer<br/>Best frame on gesture"]
                GestCrop["<b>Gesture + crop</b><br/>Detect · crop lock"]
                Mic["<b>Mic & display</b>"]
            end
        end

        subgraph Mobile["<b><span style='font-size:20px'>Mobile phone</span></b>"]
            direction TB
            VA["<b>Voice assistant</b><br/>Optional voice · hotword · follow-up · TTS · UI"]
            subgraph SSGC["<b><span style='font-size:16px'>SS-Glasses-Core</span></b>"]
                direction TB
                PolicyVQ["<b>EventPolicy:VisualQA</b><br/>Gesture gates · stability · negatives"]
                OrchVQ["<b>EventOrchestrator:VisualQA</b><br/>Crop"]
                OEM["<b>OEM SDK</b><br/>gRPC · glasses transport"]
            end
            GGGC["<b>GG-Glasses-Core</b><br/>Cloud session · auth · multimodal client"]
        end

        CloudVLM["<b>Cloud VLM / LLM</b><br/>Multimodal Q&A"]
    end

    subgraph BottomDeck[" "]
        direction RL
        subgraph LegendRow["Legend"]
            direction LR
            LgG("<b>Glasses</b>")
            LgM("<b>Voice</b>")
            LgGG("<b>GG-Core</b>")
            LgOEM("<b>OEM</b>")
            LgS("<b>SS-Core</b>")
            LgC("<b>Cloud</b>")
            LgG --- LgM --- LgGG --- LgOEM --- LgS --- LgC
        end
    end

    MainRow ~~~ BottomDeck

    User --> TwoHand
    TwoHand --> GestCrop
    Cam -->|"frames"| GestCrop
    GestCrop -->|"① gesture + crop"| OrchVQ
    OrchVQ <-->|"② policy"| PolicyVQ

    OrchVQ -->|"③ handoff"| OEM
    OEM -->|"④ bundled request"| GGGC
    GGGC -->|"⑤ multimodal query"| CloudVLM
    CloudVLM -->|"⑥ answer"| GGGC
    GGGC -->|"⑦ answer"| OEM
    OEM -->|"⑧ gRPC response"| Mic

    OrchVQ -->|answer text| VA
    VA -->|cloud intent · transcript| OrchVQ

    User --> Mic
    Mic -. speech .-> VA
    VA -->|TTS · UI| Mic
    VA -->|follow-up| OrchVQ

    classDef glasses fill:#cfe4f5,stroke:#3a6fa0,color:#111,stroke-width:2px
    classDef mobile fill:#fde8cc,stroke:#b86a2b,color:#111,stroke-width:2px
    classDef ggCore fill:#faead4,stroke:#b87a30,color:#111,stroke-width:2px
    classDef oemSdk fill:#e0d5e8,stroke:#7a5a9a,color:#111,stroke-width:2px
    classDef ssCoreLight fill:#e6def0,stroke:#7e57c2,color:#111,stroke-width:2px
    classDef cloudVlm fill:#d4c8e6,stroke:#6b4ea3,color:#111,stroke-width:2px
    classDef userGesture fill:#e1edf5,stroke:#4a7aa3,color:#111,stroke-width:2px
    classDef legendGlasses fill:#cfe4f5,stroke:#3a6fa0,color:#111
    classDef legendMobile fill:#fde8cc,stroke:#b86a2b,color:#111
    classDef legendGG fill:#faead4,stroke:#b87a30,color:#111
    classDef legendOEM fill:#e0d5e8,stroke:#7a5a9a,color:#111
    classDef legendSS fill:#e6def0,stroke:#7e57c2,color:#111
    classDef legendCloud fill:#d4c8e6,stroke:#6b4ea3,color:#111
    classDef colHidden fill:none,stroke:none,color:#111

    style SSGC fill:#e6def0,stroke:#7e57c2,stroke-width:2px
    style GCol fill:none,stroke:none
    style Glasses stroke:#3a6fa0,stroke-width:2px
    style Mobile stroke:#b86a2b,stroke-width:2px
    style MainRow fill:none,stroke:none
    style BottomDeck fill:none,stroke:none
    style LegendRow fill:#fafafa,stroke:#bdbdbd,color:#111,stroke-width:1px

    class TwoHand userGesture
    class Cam,Mic,GestCrop glasses
    class VA mobile
    class GGGC ggCore
    class OEM oemSdk
    class PolicyVQ,OrchVQ ssCoreLight
    class CloudVLM cloudVlm
    class GCol colHidden

    class LgG legendGlasses
    class LgM legendMobile
    class LgGG legendGG
    class LgOEM legendOEM
    class LgS legendSS
    class LgC legendCloud
```
