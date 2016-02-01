# Change Log
All notable changes to this project will be documented in this file. This
change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### TODO
- Capture Criterium output
    + flags:
        * flag for debug output (-D)
        * flag for warn output (-W) - rebind `*report-warn*`, also several `(println "WARNING...")` statements
        * flag for progress output (-P) - already exists
    + rebind `*out*` and copy whatever is written through the `boot.util/info`
      machinery

## 0.1.0 - 2016-02-01
### Added
- Initial release

[Unreleased]: https://github.com/tulos/boot-criterium/compare/0.1.0...HEAD
