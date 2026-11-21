"""Finds asset files whose paths have upper case letters (26.3 skips them, and they break on case-sensitive file
systems), works out a snake_case name for each, and lists every place that names them.

    python tools/port-26.3/lowercase_assets.py           # report only
    python tools/port-26.3/lowercase_assets.py --apply   # git mv the files and rewrite the references
    python tools/port-26.3/lowercase_assets.py --apply --delete-unused   # also git rm files nothing names

References are searched as resource names (namespace:path without the textures/ or models/ prefix and without the
extension) and as bare paths, in Java sources, resources (json, md, mcmeta, txt, cfg) and the port tools.
"""
import os
import re
import subprocess
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', '..'))
RES = os.path.join(ROOT, 'buildcraft_resources')
ASSETS = os.path.join(RES, 'assets')
SEARCH_DIRS = ['common', 'BuildCraftAPI', 'buildcraft_resources', 'tools']
SEARCH_EXT = ('.java', '.json', '.md', '.mcmeta', '.txt', '.cfg', '.py', '.toml')
TYPE_DIRS = ('textures', 'models', 'blockstates')


def snake(segment):
    """camelCase -> camel_case; already-lowercase parts are kept."""
    s = re.sub(r'(?<=[a-z0-9])([A-Z])', r'_\1', segment)
    s = re.sub(r'(?<=[A-Z])([A-Z][a-z])', r'_\1', s)
    return s.lower()


def find_files():
    out = []
    for dirpath, _, files in os.walk(ASSETS):
        for f in files:
            full = os.path.join(dirpath, f)
            rel = os.path.relpath(full, ASSETS).replace(os.sep, '/')
            parts = rel.split('/')
            if len(parts) < 3 or parts[1] == 'lang':
                continue
            if re.search('[A-Z]', '/'.join(parts[1:])):
                out.append(rel)
    return sorted(out)


def resource_names(rel):
    """Names this file can be referred to by: ns:path (no type dir, no extension) and the path inside the type dir."""
    parts = rel.split('/')
    ns, typ, rest = parts[0], parts[1], '/'.join(parts[2:])
    base = re.sub(r'\.(png|json)(\.mcmeta)?$', '', rest)
    names = {ns + ':' + base}
    if typ in TYPE_DIRS:
        names.add(base)  # e.g. "blocks/someDir/someTexture" inside the same namespace
    names.add(ns + ':' + typ + '/' + rest)  # full resource location, e.g. ns:textures/blocks/x.png
    names.add(typ + '/' + rest)
    return names


def load_sources():
    texts = {}
    for d in SEARCH_DIRS:
        for dirpath, dirnames, files in os.walk(os.path.join(ROOT, d)):
            if 'src_old' in dirpath:
                continue
            for f in files:
                if f.endswith(SEARCH_EXT) and f != os.path.basename(__file__):
                    p = os.path.join(dirpath, f)
                    try:
                        texts[p] = open(p, encoding='utf-8').read()
                    except UnicodeDecodeError:
                        pass
    return texts


def ref_pattern(name):
    # whole-name match: not preceded/followed by more path characters
    return re.compile(r'(?<![A-Za-z0-9_/:.-])' + re.escape(name) + r'(?![A-Za-z0-9_/-])')


def main():
    apply = '--apply' in sys.argv
    delete_unused = '--delete-unused' in sys.argv
    files = find_files()
    texts = load_sources()
    plan = []
    for rel in files:
        parts = rel.split('/')
        new_rel = '/'.join(parts[:2] + [snake(p) for p in parts[2:]])
        refs = []
        for name in sorted(resource_names(rel), key=len, reverse=True):
            pat = ref_pattern(name)
            for p, t in texts.items():
                if p.endswith(os.path.join('assets', *rel.split('/'))):
                    continue
                for m in pat.finditer(t):
                    line = t.count('\n', 0, m.start()) + 1
                    refs.append((name, os.path.relpath(p, ROOT), line))
        plan.append((rel, new_rel, refs))

    for rel, new_rel, refs in plan:
        print(('USED  ' if refs else 'UNUSED') + ' ' + rel + ' -> ' + new_rel)
        for name, p, line in refs:
            print('        %s:%d  %s' % (p, line, name))

    if not apply:
        return
    for rel, new_rel, refs in plan:
        src = os.path.join(ASSETS, *rel.split('/'))
        if not refs and delete_unused:
            subprocess.check_call(['git', 'rm', '-q', src], cwd=ROOT)
            continue
        dst = os.path.join(ASSETS, *new_rel.split('/'))
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        subprocess.check_call(['git', 'mv', src, dst], cwd=ROOT)
        old_names = resource_names(rel)
        new_names = resource_names(new_rel)
        mapping = {}
        for o in old_names:
            for n in new_names:
                # pair names of the same form (same prefix shape)
                if o.lower().replace('_', '') == n.lower().replace('_', '') and o.count(':') == n.count(':') \
                        and o.count('/') == n.count('/'):
                    mapping[o] = n
        for p in set(os.path.join(ROOT, r[1]) for r in refs):
            t = open(p, encoding='utf-8').read()
            for o, n in sorted(mapping.items(), key=lambda x: -len(x[0])):
                t = ref_pattern(o).sub(n, t)
            open(p, 'w', encoding='utf-8', newline='').write(t)


if __name__ == '__main__':
    main()
