# ProjectExpansion 1.21.1 - Enhanced Edition

An enhanced version of [Project Expansion](https://github.com/DonovanDMC/ProjectExpansion) with additional features and optimizations for Minecraft 1.21.1.

**Note**: This is a community-enhanced fork. For the original project, please visit the [original repository](https://github.com/DonovanDMC/ProjectExpansion).

## ✨ New Features

### 🧙 Arcane Tablet is BACK / 移植添加了奥术转化桌
- **Just Like the Old time**: Crafting, searching, JEI features implemented 
- **功能照旧**: 实现物品合成、搜索、与JEI的功能

### 🔒 Security & Performance / 安全与性能
- **Pinyin Search Support**: Search for items using Chinese characters, full pinyin, or pinyin initials
- **拼音搜索支持**: 支持使用中文字符、全拼音或拼音首字母搜索物品
- **Rate Limiting**: EMC extraction protected against abuse (20 operations/second)
- **速率限制**: 对EMC的使用过程设置上限（每秒20次操作）
- **Memory Optimization**: Circular buffer implementation 
- **内存优化**: 设置循环缓冲区
- **Network Security**: Input validation and safe data serialization
- **网络安全**: 输入白名单和安全数据序列
- **Crash Prevention**: Comprehensive error handling and edge case protection
- **崩溃预防**: 错误处理和额外防护

## 🔧 Development

### Building from Source
```bash
git clone https://github.com/Laurel2718/ProjectExpansion-Enhanced.git
cd ProjectExpansion-Enhanced
./gradlew build
```
### Dependencies
- Minecraft 1.21.1
- NeoForge 21.1.148+
- ProjectE 1.1.0+
- JEI 19.21.0.247+ (optional, for enhanced JEI integration)

## 📋 License

This project is licensed under the [GNU Lesser General Public License v3 (LGPLv3)](LICENSE).

### License Attribution
- **Enhanced version**: Copyright (c) 2025 Laurel2718
- **Base ExtendedExchange**: Copyright FTB Team (LGPLv3)
- **Original Project Expansion**: Copyright (c) 2025 Donovan_DMC (MIT)

This license ensures compatibility with the base ExtendedExchange project while maintaining the freedom to use and modify the code.

### Third-Party Licenses
- ExtendedExchange (LGPLv3): Core project structure and JEI integration
- PinIn Library (LGPL-3.0): Pinyin conversion functionality
- ProjectE API (MIT): EMC and knowledge system integration
- Minecraft/NeoForge: Subject to Mojang EULA and NeoForge licensing

## ⚠️ Disclaimer

This mod is not officially endorsed by ProjectE, JustEnoughCharacters, or any other referenced projects. 
For support, please [open an issue](https://github.com/Laurel2718/ProjectExpansion-Enhanced/issues) 
on this repository.

## 📞 Support

- **Issues**: [GitHub Issues](https://github.com/Laurel2718/ProjectExpansion-Enhanced/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Laurel2718/ProjectExpansion-Enhanced/discussions)