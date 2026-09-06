#define MyAppName "WinGo"
#define MyAppVersion "1.0"
#define MyAppExeName "WinGo.exe"

[Setup]
AppName={#MyAppName}
AppVersion={#MyAppVersion}
DefaultDirName={tmp}\{#MyAppName}
CreateAppDir=no
OutputDir=output
OutputBaseFilename=WinGo-Portable-{#MyAppVersion}
PrivilegesRequired=lowest
ArchitecturesAllowed=x64
ArchitecturesInstallIn64BitMode=x64
Compression=lzma2
SolidCompression=yes
Uninstallable=no
DisableProgramGroupPage=yes

[Files]
Source: "dist\WinGo\*"; DestDir: "{tmp}\{#MyAppName}"; Flags: recursesubdirs createallsubdirs

[Run]
Filename: "{tmp}\{#MyAppName}\{#MyAppExeName}"; Flags: waituntilterminated

[Code]
procedure DeinitializeSetup();
begin
  DelTree(ExpandConstant('{tmp}\{#MyAppName}'), True, True, True);
end;