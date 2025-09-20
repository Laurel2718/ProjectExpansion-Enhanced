# Attribution & Third-Party Code

This project incorporates code, patterns, and inspiration from various open source projects. 
We are grateful to all contributors and maintainers of these projects.

## 🏗️ Primary Dependencies

### Project Expansion (Base)
- **Repository**: https://github.com/DonovanDMC/ProjectExpansion
- **Author**: Donovan_DMC
- **License**: MIT
- **Usage**: Core mod structure, base functionality, and mod framework
- **Files**: Majority of the codebase structure and initial implementations

### ProjectE API
- **Repository**: https://github.com/sinkillerj/ProjectE  
- **Author**: sinkillerj and contributors
- **License**: MIT
- **Usage**: EMC system integration, knowledge provider API, item info API
- **Files**: API interfaces and integration patterns throughout the project

## 🔍 Arcane Tablet Implementation

### ExtendedExchange (Core Base & JEI Integration)
- **Repository**: https://github.com/FTBTeam/ExtendedeXchange
- **Authors**: FTB Team
- **License**: GNU Lesser General Public License v3 (LGPLv3)
- **Usage**: Core project structure, JEI recipe transfer patterns, GUI implementation, Arcane Tablet functionality
- **Files**: JEI integration classes, container handlers, GUI patterns
- **Description**: An addon to ProjectE adding useful extra blocks and items

### Pinyin4j Library
- **Repository**: https://sourceforge.net/projects/pinyin4j/
- **Author**: Li Min (original), various contributors
- **License**: LGPL-2.1
- **Usage**: Core pinyin conversion functionality
- **Files**: External dependency for Chinese character to pinyin conversion

## 🖼️ Assets & Resources

### Matter Block Textures
- **Creator**: @32polaris (Discord)
- **License**: Custom permission (used with permission)
- **Files**: `src/main/resources/assets/projectexpansion/textures/block/matter/`

## 🔧 Development Tools & Patterns

### NeoForge Development Patterns
- **Repository**: https://github.com/neoforged/NeoForge
- **License**: LGPL-2.1
- **Usage**: Mod development patterns, container systems, network handling

### Minecraft Forge/NeoForge Documentation
- **Source**: Official NeoForge documentation and community examples
- **Usage**: Best practices for GUI development, data components, mixins

## 🛡️ Security & Performance Inspirations

### Rate Limiting Patterns
- **Inspiration**: Industry-standard rate limiting implementations
- **Usage**: EMC extraction rate limiting system
- **Files**: ExtractionTracker class in ContainerArcaneTablet

### Caching Strategies  
- **Inspiration**: Common LRU cache implementations and performance optimization patterns
- **Usage**: Pinyin search result caching
- **Files**: Cache management in PinYinUtils

## 📄 License Compatibility

This project is licensed under GNU LGPLv3 to maintain compatibility with key dependencies:

- **LGPL v3 Licensed**: ExtendedExchange (base structure), NeoForge framework
- **LGPL v2.1 Licensed**: Pinyin4j, JustEnoughCharacters (PinIn) 
- **MIT Licensed**: Project Expansion (original), ProjectE API
  - MIT is compatible with LGPL and allows incorporation
- **Custom Permission**: Asset files used with explicit permission

## 🙏 Acknowledgments

Special thanks to:
- **FTB Team** for creating ExtendedExchange and providing the core foundation for this enhanced version
- **Donovan_DMC** for creating the original Project Expansion
- **Towdium** for JustEnoughCharacters and the PinIn library
- **sinkillerj** and ProjectE team for the excellent ProjectE API
- **@32polaris** for the beautiful matter block textures
- **The NeoForge team** for the modding framework
- **The Minecraft modding community** for shared knowledge and best practices

## 📝 Reporting Attribution Issues

If you believe we've missed attributing your work or if you have concerns about 
how your code is being used, please [open an issue](https://github.com/Laurel2718/ProjectExpansion-Enhanced/issues) 
and we'll address it promptly.

---

This file is maintained to ensure proper credit is given to all contributors to the open source ecosystem that made this project possible.


