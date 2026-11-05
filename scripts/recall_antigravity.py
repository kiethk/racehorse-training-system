"""
Helper utility to list, search, and recall Antigravity desktop conversation history
related to the current repository or workspace.
"""

import os
import sys
import json
import glob
import re
import io
from datetime import datetime

# Ensure utf-8 output on Windows console
if sys.platform == "win32":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")

ANTIGRAVITY_BASE = r"C:\Users\LENOVO\.gemini\antigravity"
BRAIN_DIR = os.path.join(ANTIGRAVITY_BASE, "brain")
ANNOTATIONS_DIR = os.path.join(ANTIGRAVITY_BASE, "annotations")
DEFAULT_KEYWORD = "racehorse-training-system"

def get_session_title(session_id):
    """Retrieve session title from annotations pbtxt if available."""
    annot_file = os.path.join(ANNOTATIONS_DIR, f"{session_id}.pbtxt")
    if os.path.exists(annot_file):
        try:
            with open(annot_file, "r", encoding="utf-8", errors="ignore") as f:
                content = f.read()
                m = re.search(r'title:\s*"([^"]+)"', content)
                if m:
                    return m.group(1)
        except Exception:
            pass
    return None

def find_sessions(keyword=DEFAULT_KEYWORD):
    """Scan sessions in brain directory that reference the keyword."""
    if not os.path.exists(BRAIN_DIR):
        print(f"Directory not found: {BRAIN_DIR}")
        return []

    results = []
    for entry in os.scandir(BRAIN_DIR):
        if not entry.is_dir():
            continue
        session_id = entry.name
        transcript_path = os.path.join(entry.path, ".system_generated", "logs", "transcript.jsonl")
        if not os.path.exists(transcript_path):
            continue

        try:
            mtime = os.path.getmtime(transcript_path)
            time_str = datetime.fromtimestamp(mtime).strftime("%Y-%m-%d %H:%M:%S")

            is_match = False
            with open(transcript_path, "r", encoding="utf-8", errors="ignore") as f:
                header = f.read(8192)
                if keyword.lower() in header.lower():
                    is_match = True

            if is_match:
                title = get_session_title(session_id)
                if not title:
                    with open(transcript_path, "r", encoding="utf-8", errors="ignore") as f:
                        for line in f:
                            if '"USER_INPUT"' in line:
                                try:
                                    data = json.loads(line)
                                    title = (data.get("content") or "").strip().replace("\n", " ")[:70]
                                    break
                                except Exception:
                                    pass

                results.append({
                    "session_id": session_id,
                    "title": title or "Untitled Session",
                    "time": time_str,
                    "mtime": mtime,
                    "path": entry.path
                })
        except Exception:
            continue

    results.sort(key=lambda x: x["mtime"], reverse=True)
    return results

def show_session(session_id, max_turns=10):
    """Print the turns and artifacts of a given session."""
    session_dir = os.path.join(BRAIN_DIR, session_id)
    if not os.path.exists(session_dir):
        print(f"Session directory not found: {session_dir}")
        return

    print(f"=== Session: {session_id} ===")
    title = get_session_title(session_id)
    if title:
        print(f"Title: {title}")

    # List markdown artifacts
    md_files = glob.glob(os.path.join(session_dir, "*.md"))
    if md_files:
        print("\nArtifacts:")
        for mf in md_files:
            print(f" - {os.path.basename(mf)}")

    # Extract user inputs from transcript
    transcript_path = os.path.join(session_dir, ".system_generated", "logs", "transcript.jsonl")
    if os.path.exists(transcript_path):
        print("\nRecent User Requests:")
        turn_count = 0
        with open(transcript_path, "r", encoding="utf-8", errors="ignore") as f:
            for line in f:
                if '"USER_INPUT"' in line:
                    try:
                        data = json.loads(line)
                        content = data.get("content", "").strip()
                        turn_count += 1
                        print(f"[{turn_count}] User: {content[:150]}{'...' if len(content) > 150 else ''}")
                        if turn_count >= max_turns:
                            break
                    except Exception:
                        pass

def search_text(query, keyword=DEFAULT_KEYWORD):
    """Search for specific topic across repo sessions."""
    sessions = find_sessions(keyword)
    print(f"Searching for '{query}' across {len(sessions)} sessions...")
    found_any = False
    for s in sessions:
        transcript_path = os.path.join(s["path"], ".system_generated", "logs", "transcript.jsonl")
        try:
            with open(transcript_path, "r", encoding="utf-8", errors="ignore") as f:
                for line in f:
                    if query.lower() in line.lower() and '"USER_INPUT"' in line:
                        data = json.loads(line)
                        print(f"\n[Session {s['session_id']}] ({s['time']}) - {s['title']}")
                        print(f"  Match: {data.get('content', '').strip()[:200]}")
                        found_any = True
                        break
        except Exception:
            continue
    if not found_any:
        print("No matches found in user requests.")

if __name__ == "__main__":
    cmd = sys.argv[1] if len(sys.argv) > 1 else "list"
    if cmd == "list":
        keyword = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_KEYWORD
        sessions = find_sessions(keyword)
        print(f"Found {len(sessions)} Antigravity sessions related to '{keyword}':\n")
        for idx, s in enumerate(sessions, 1):
            print(f"{idx}. [{s['time']}] {s['session_id']}")
            print(f"   Title: {s['title']}")
    elif cmd == "show":
        if len(sys.argv) < 3:
            print("Usage: python recall_antigravity.py show <session_id>")
        else:
            show_session(sys.argv[2])
    elif cmd == "search":
        if len(sys.argv) < 3:
            print("Usage: python recall_antigravity.py search <query>")
        else:
            search_text(sys.argv[2])
    else:
        print("Usage: python recall_antigravity.py [list|show <id>|search <text>]")
