[CmdletBinding()]
param(
    [ValidateSet("low", "medium", "high", "xhigh", "max")]
    [string]$ReasoningEffort = "low"
)

$repositoryRoot = Split-Path -Parent $PSScriptRoot

codex `
    --cd $repositoryRoot `
    --profile "fortresstv" `
    --config "model_reasoning_effort=$ReasoningEffort"
