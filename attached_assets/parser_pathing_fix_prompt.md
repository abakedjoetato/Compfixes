# AI REPLIT SESSION PROMPT — PARSER PATHING & STAT CORRUPTION INVESTIGATION

## OBJECTIVE

This session addresses a critical issue where the bot’s **parsers fail to locate `.csv` or `Deadside.log` files**, potentially due to misconfigured pathing or structural changes in server configuration storage.

You must perform a deep investigation and fix all causes preventing:
- File discovery
- File parsing
- Stat ingestion
- Stat embed output

---

## PHASE 0 — PROJECT INIT (REQUIRED)

1. **Unzip** the uploaded `.zip` file from the attached assets  
2. **Move** all contents from the unzipped directory to the **project root**  
3. **Clean up**:
   - Remove any nested or duplicate folders (e.g., `project/`, `DeadsideBot/`)  
   - Delete empty folders or broken symbolic links  

4. **Scan and log** the following:
   - Parser classes for `.csv` and `Deadside.log`  
   - Server configuration classes  
   - File discovery routines (SFTP, path builders, connectors)  
   - Stat storage handlers  
   - Leaderboard/stat embed formatters  

5. Detect or create a `.env` or `config.properties` file  
6. Load secrets from Replit:
   - `BOT_TOKEN`  
   - `MONGO_URI`  

7. Start the bot using **JDA 5.x** and confirm:
   - Discord and MongoDB connect without error  
   - All startup routines initialize correctly  

---

## PHASE 1 — PARSER PATH VALIDATION & FILE RESOLUTION

### Investigative Objectives:

- [ ] Audit all logic that builds or navigates SFTP paths  
- [ ] Validate `.csv` files are searched under:  
  `./{host}_{server}/actual1/deathlogs/`  
- [ ] Validate `Deadside.log` is located under:  
  `./{host}_{server}/Logs/`  
- [ ] Ensure filename discovery supports:
  - New server naming conventions  
  - File name rotation (log rollovers)  
  - Directory name consistency  

### Root Cause Checklist:

- [ ] Determine if server configuration structure has changed:
  - Are paths stored differently?  
  - Are host/server variables malformed?  
  - Is the parser referencing outdated config fields?  
- [ ] Validate file discovery routines (SFTP walk, glob, filename match)  
- [ ] Confirm there is no hardcoded override or default fallback failure  
- [ ] Ensure servers without files return cleanly without logging false errors  

---

## PHASE 2 — STAT INGESTION & EMBED ACCURACY VALIDATION

### Audit and Correct:

- [ ] Ensure parsed lines are stored:
  - With correct guild and server scoping  
  - In the correct schema (Player, Killfeed, WeaponStats, etc.)
- [ ] Confirm no lines are silently skipped due to malformed fields  
- [ ] Match parsed record count to lines in sample `.csv`  
- [ ] Recheck stat categories:
  - Kills, deaths, KDR  
  - Weapon stats  
  - Suicide logic  
  - Faction aggregation  

### Validate Embed Outputs:

- [ ] All stat-based embeds must reflect parsed data  
- [ ] Confirm:
  - Killfeed entries  
  - Leaderboards  
  - Personal stats  
- [ ] Ensure embed format, logo, and layout follow standards  
- [ ] No outdated data or fallback hardcoding permitted  

---

## COMPLETION CRITERIA

- [✓] All file paths are resolved and discovered successfully  
- [✓] Parsers read expected files with no skips or path failures  
- [✓] Stats are created, stored, and calculated correctly  
- [✓] All embeds reflect correct and recent data  
- [✓] Bot compiles, connects, and performs live queries correctly

---

## EXECUTION POLICY — STRICT

- No speculative commits, logs, or checkpoints until final validation  
- Trial and error is forbidden — all logic must be confirmed by runtime evidence  
- Phase must be completed in a single atomic batch  
- Output only when the full system works as intended  
