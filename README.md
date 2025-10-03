# Simp — compact, sleek Minecraft client (Recode)

<p align="center">
  <a href="https://github.com/shxzu/Simp/releases"><img alt="releases" src="https://img.shields.io/github/v/release/shxzu/Simp?style=for-the-badge"></a>
  <a href="https://img.shields.io/github/stars/shxzu/Simp?style=for-the-badge"><img alt="stars" src="https://img.shields.io/github/stars/shxzu/Simp?style=for-the-badge"></a>
  <a href="https://img.shields.io/github/forks/shxzu/Simp?style=for-the-badge"><img alt="forks" src="https://img.shields.io/github/forks/shxzu/Simp?style=for-the-badge"></a>
</p>

---

## ✨ About

**Simp** is a modern, compact, and visually-sleek Minecraft hacked client UI and codebase — **this branch is a recode** focused on performance, maintainability, and a refined UI/UX. Built with **LWJGL 3.3.6**, the client aims to provide a snappy, modular, and extensible platform for client-side quality-of-life features and offline and online/customization tooling.

> ⚠️ **Disclaimer:** This project is NOT meant to hurt any Anti-Cheat or server owners! This is simply just a utility client that allows players to gain an advantage against others in servers. I DO NOT condone cheating I just enjoy making them!

---

## 💎 Highlights

- Recoded from the ground up — cleaner architecture, modular system, improved maintainability.
- Sleek, compact GUI: minimal, modern, and space-efficient.
- Built on LWJGL **3.3.6** (OpenGL / input / native bindings).
- Modular subsystem: independant modules with properties (bool, enum, number).
- Fast startup and lightweight runtime footprint.
- Developer-friendly: clear module API, test harness, and example modules.

---

## 🧭 Features

- Compact click GUI with property sidebar (boolean / enum / number).
- HUD customization: position, scale, font, opacity.
- Performance overlays: FPS, memory, tick time.
- Config system: save/load profiles (JSON).
- Keybinding manager with conflict detection.
- Functional module system: combat, movement, player, and many other categories of modules to use from.

---

## 🛠️ Tech stack

- Java 22 (or higher)  
- LWJGL 3.3.6  
- Maven (recommended)  
- IDE (preferably Intellij IDEA)

---

## 📦 Installation / Build

# Clone the repo
```bash
git clone https://github.com/shxzu/Simp.git

# Open in IDE and build the Maven project
(Intellij IDEA Instructions)

Click the import Maven project button when opening the project folder in your IDE.

(Anything other IDE isn't supported but if you find a way to import it using Eclipse, NetBeans, e.g. go ahead.)

# Make a run configuration
Make sure your run directory has the "/workspace" at the end of your path!! Example: (Working Directory: C:\Users\shxzu\Documents\GitHub\Simp\workspace)
