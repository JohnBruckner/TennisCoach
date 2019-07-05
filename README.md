# TennisCoach
Android app that ranks tennis strokes.
Implementation based on research done by another student at the uni.

The app uses a database of strokes performed and rated by a professional coach. These are then 
used as a baseline for subsequent strokes.
Each shot is transformed into a string of letters using the Symbolic Aggregate Approximation
and then using a distance function a distance from a "good stroke" is calculated based in which 
the shot is ranked.
