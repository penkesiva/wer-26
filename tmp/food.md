flowchart LR
    User([User])
 
    subgraph GlassesNode["Glasses (OEM)"]
      direction TB
      Cam["Camera @ low FPS"]
      EatDet["On-device eating detector<br/>(food/plate/utensils +<br/>hand-to-mouth motion)"]
      MicDisp["Mic + Display"]
    end
 
    subgraph Mobile["Mobile"]
      direction TB
      subgraph SSGC["SS-Glasses-Core"]
        OEM["OEM SDK"]
        Orchestrator["Meal-event orchestrator"]
      end
      GGGC["GG-Glasses-Core<br/>(Gemini session,<br/>phrases voice)"]
      AMEM["Ambient-Memory<br/>meal entries:<br/>photos + items + macros"]
    end
 
    NutritionAPI["3rd-Party Nutrition API<br/>(cuisine, calories, macros,<br/>per-item breakdown)"]
    Gemini["Cloud: Gemini VLM/LLM"]
    UserCloud["User-account Cloud<br/>(optional sync,<br/>cross-day history)"]
 
    %% Capture phase
    EatDet -- "eating detected (vision)" --> OEM
    Cam -- "meal photo (gRPC)" --> OEM
    SSGC -- "photo" --> NutritionAPI
    NutritionAPI -- "items + macros" --> SSGC
    SSGC -- "write meal entry" --> AMEM
 
    %% Voice responses
    SSGC -- "meal bundle" --> GGGC
    GGGC -- "phrase request" --> Gemini
    Gemini -- "ack/summary/nudge text" --> GGGC
    GGGC -- "voice response" --> SSGC
    SSGC -- "audio (gRPC)" --> MicDisp
 
    %% User query
    User -- "What did I eat today?" --> MicDisp
    MicDisp -- "voice query (gRPC)" --> OEM
    SSGC -- "fetch today's meals" --> AMEM
 
    %% Optional sync
    AMEM -. "opt-in sync" .-> UserCloud
