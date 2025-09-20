# Contributing to ProjectExpansion Enhanced

Thank you for your interest in contributing! This document provides guidelines for contributing to this project.

## 🚀 Getting Started

### Prerequisites
- Java 21+
- Git
- IDE (IntelliJ IDEA recommended)
- Basic knowledge of Minecraft modding with NeoForge

### Setup Development Environment

1. **Clone the repository**
   ```bash
   git clone https://github.com/Laurel2718/ProjectExpansion-Enhanced.git
   cd ProjectExpansion-Enhanced
   ```

2. **Import into IDE**
   - Open the project in IntelliJ IDEA
   - Let Gradle sync complete
   - Ensure Java 21 is selected

3. **Test the build**
   ```bash
   ./gradlew build
   ```

4. **Run in development**
   ```bash
   ./gradlew runClient
   ```

## 📋 Types of Contributions

We welcome the following types of contributions:

### 🐛 Bug Fixes
- Fix crashes or gameplay issues
- Resolve performance problems
- Security vulnerability patches

### ✨ New Features  
- Additional items or blocks
- Quality of life improvements
- Integration with other mods

### 🔧 Technical Improvements
- Performance optimizations
- Code refactoring
- Documentation improvements

### 🌍 Localization
- Translation support for new languages
- Improvements to existing translations

## 🔄 Contribution Workflow

### 1. Create an Issue (Optional but Recommended)
For significant changes, please create an issue first to discuss:
- What you want to implement
- Why it's needed  
- How you plan to implement it

### 2. Fork & Branch
```bash
# Fork the repository on GitHub, then:
git clone https://github.com/Laurel2718/ProjectExpansion-Enhanced.git
cd ProjectExpansion-Enhanced
git checkout -b feature/your-feature-name
```

### 3. Make Changes
- Follow the existing code style
- Add comments for complex logic
- Include security considerations for network code
- Write efficient code for performance-critical paths

### 4. Test Your Changes
```bash
# Build and test
./gradlew build
./gradlew runClient

# Test in multiplayer if applicable
./gradlew runServer
```

### 5. Commit & Push
```bash
git add .
git commit -m "feat: add your feature description"
git push origin feature/your-feature-name
```

### 6. Create Pull Request
- Use a clear title and description
- Reference related issues with `#issue-number`
- Explain what your changes do and why

## 🚫 What NOT to Contribute

- **Overpowered items**: Maintain game balance
- **Complex dependencies**: Avoid unnecessary external libraries  
- **Breaking changes**: Don't break existing functionality
- **Copyrighted content**: Only original or properly licensed content

## 📚 Resources

### Documentation
- [NeoForge Documentation](https://docs.neoforged.net/)
- [ProjectE API Documentation](https://github.com/sinkillerj/ProjectE/wiki)
- [Minecraft Wiki](https://minecraft.wiki/)

### Community
- [GitHub Discussions](https://github.com/Laurel2718/ProjectExpansion-Enhanced/discussions)
- [NeoForge Discord](https://discord.neoforged.net/)

## 📄 License

By contributing to this project, you agree that your contributions will be licensed under the LGPL v3 License.
