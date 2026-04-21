# Skill: moqui-scaffold
**Description**: Scaffolds a new Moqui UI module or screen (Parent, Find, and Detail screens).

## Execution Steps
Use your native file-writing capabilities to generate three XML files in the target directory. Replace `[Name]` with the requested entity name (PascalCase) and `[name]` with the camelCase version.

1. **Create `[Name].xml` (Parent):**
   - Write the `<screen>` root with transitions to `find[Name]` and `[Name]Detail`.
   - Add `<subscreens default-item="Find[Name]">`.
   - Wrap the `<subscreens-panel>` in a `<section>` checking `condition="[name]Id && !['Find[Name]', '[Name]'].contains(sri.screenUrlInfo.targetScreen?.getScreenName())"`.

2. **Create `Find[Name].xml` (List):**
   - Add `<entity-find>` in the `<actions>` block.
   - Build a `<form-list>` with a `<container-dialog>` for creation.
   - Map the ID in row actions: `<script>[name]IdToPass = [name]Id</script>`.

3. **Create `[Name]Detail.xml` (Detail):**
   - Require the `[name]Id` parameter.
   - Fetch the record via `<entity-find-one>`.
   - Build a `<form-single>` to map and update the record. Include a back button to `lastScreenUrl`.