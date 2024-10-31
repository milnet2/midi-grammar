{
  description = "MIDI file grammars based on ANTLR";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    let
        # Define a custom list of systems you want to target.
        customSystems = [
          "x86_64-linux"
          "aarch64-linux"
          "x86_64-darwin"
          "aarch64-darwin"
        ];
    in
    flake-utils.lib.eachSystem customSystems (system:
    #flake-utils.lib.eachDefaultSystem (system:
      let
        pname = "midi-file-grammar";
        version = "0.1.0";

        pkgs = import nixpkgs {
          inherit system;
          config = {
            allowUnfree = true;
          };
        };

        common = rec {
            nativeBuildInputs = with pkgs; [
                pkg-config
                cmake
                ninja
                clang
            ];
            buildInputs = with pkgs; [
                gtest
            ];
            checkInputs = with pkgs; [
            ];
        };

        linux = rec {
            nativeBuildInputs = with pkgs; [
            ];
            buildInputs = with pkgs; [
            ];
            checkInputs = with pkgs; [
            ];
        };

        midi-file-grammar = (with pkgs; stdenv.mkDerivation {
            pname = pname;
            version = version;
            src = ./.;

            nativeBuildInputs = common.nativeBuildInputs ++ linux.nativeBuildInputs;
            buildInputs = common.buildInputs ++ linux.buildInputs;
            checkInputs = common.checkInputs ++ linux.checkInputs;

            cmakeFlags = [
                "-DUSE_SYSTEM_GOOGLETEST=1"
            ];
            # buildPhase = "make -j $NIX_BUILD_CORES";

            doCheck = false;
            checkTarget = "test";

            installPhase = ''
               mkdir -p $out/lib/${system}
               cp -vR $src/src/main/include $out/include
               pwd
               ls *.a *.so *.dylib
               set -x
               cp -v *.a *.so *.dylib $out/lib/${system}
            '';
        });
      in rec {
        defaultPackage = midi-file-grammar;

        defaultApp = flake-utils.lib.mkApp { drv = defaultPackage; };

        devShells.default = pkgs.mkShell {
          buildInputs = common.buildInputs ++ linux.buildInputs;
          nativeBuildInputs = common.nativeBuildInputs ++ linux.nativeBuildInputs;

          shellHook = ''
            echo "Environment prepared. You can now run cmake and make to build your project."
          '';
        };
      }
    );
}