# Security Policy

## Scope

This repository is a maintained fork of Google AI Edge Gallery. Security-sensitive areas for this fork include:

- the local HTTP bridge in `LlmHttpService`
- authentication and local bridge preferences in `LlmHttpPrefs`
- release and CI automation under `.github/workflows/`

## Supported versions

Security fixes are applied on a best-effort basis to the default branch (`main`) and recent tagged releases of this fork.

## Reporting a vulnerability

Do not open a public GitHub issue for suspected vulnerabilities involving:

- HTTP bridge exposure or auth bypass
- secrets, tokens, or credential handling
- release artifacts or CI compromise
- sensitive logs or private bug-report archives

Instead, report privately to the maintainer through a non-public channel available on the repository profile.

When reporting, include:

1. A short summary of the issue.
2. Impact and attack preconditions.
3. Reproduction steps or proof of concept.
4. Affected commit, tag, or branch if known.

## Disclosure policy

The preferred process is:

1. Private report.
2. Reproduction and triage.
3. Fix prepared on the fork.
4. Coordinated public disclosure after a fix or mitigation is available.

If a problem appears to belong to upstream rather than this fork, it may need to be reported upstream as well.
