# ⚡ English Quest

Application Android pour progresser en anglais (B1 → B2, objectif bac), 100 % hors ligne, sans compte et sans pub.

- 517 mots et expressions du bac répartis en 10 thèmes, 20 leçons de grammaire (160 exercices)
- Répétition espacée (SM-2) : ce que tu rates revient plus souvent
- QCM, traduction FR→EN et EN→FR, texte à trous, dictée (synthèse vocale), paires, phrases à remettre dans l'ordre, chasse à l'erreur
- XP, niveaux, rangs, séries + gels, combos, badges (dont cachés), coffres, boss, contre-la-montre, survie, statistiques, rappel quotidien

---

## Obtenir l'APK sans rien installer (GitHub Actions)

1. **Crée un compte** sur <https://github.com> (gratuit).
2. En haut à droite : **+ › New repository**. Nom : `english-quest`. Coche **Private** si tu veux. Clique sur **Create repository**.
3. Sur la page du dépôt vide, clique sur le lien **uploading an existing file**.
4. **Dézippe** `EnglishQuest.zip` sur ton ordinateur, ouvre le dossier `EnglishQuest`, sélectionne **tout son contenu** (le dossier `.github`, `app`, `gradle`, et les fichiers) et glisse-le dans la page GitHub.
   - Sur Mac, les dossiers commençant par un point sont cachés : appuie sur `Cmd + Maj + .` dans le Finder pour voir `.github`.
   - Vérifie que GitHub liste bien `.github/workflows/build-apk.yml` : c'est ce fichier qui lance la compilation.
5. En bas, clique sur **Commit changes**.
6. Va dans l'onglet **Actions**. Un travail « Build APK » démarre tout seul (rond jaune). Patiente 5 à 8 minutes jusqu'à la coche verte ✅.
7. Récupère l'APK :
   - **Depuis ton téléphone** (le plus simple) : ouvre ton dépôt GitHub dans le navigateur du téléphone (connecté à ton compte), section **Releases** à droite (ou en bas sur mobile), puis touche **EnglishQuest.apk**.
   - **Ou depuis l'ordinateur** : Actions › clique sur le build › en bas, **Artifacts › EnglishQuest-apk** (c'est un zip qui contient l'APK).

Si le build échoue (croix rouge ❌) : clique dessus, ouvre l'étape en rouge, copie les dernières lignes d'erreur et envoie-les-moi.

## Installer l'APK sur le téléphone

1. Ouvre le fichier `EnglishQuest.apk` téléchargé (notification de téléchargement ou appli **Fichiers**).
2. Android affiche « Pour votre sécurité, l'installation d'applis inconnues est bloquée » : touche **Paramètres** et active **Autoriser cette source** pour ton navigateur ou l'appli Fichiers.
3. Reviens en arrière et touche **Installer**. Si Google Play Protect affiche un avertissement (appli non publiée sur le Play Store), choisis **Installer quand même** : c'est normal pour une appli que tu as compilée toi-même.
4. Lance **English Quest**, choisis ton objectif quotidien et accepte les notifications si tu veux le rappel.

**Mises à jour** : l'APK est toujours signé avec la même clé (`app/debug.keystore`), tu peux donc installer une nouvelle version par-dessus l'ancienne **sans perdre ta progression**. Ne désinstalle pas l'appli avant (ça efface la sauvegarde).

**Écoute / dictée** : si aucun son ne sort, installe « Speech Services by Google » (Synthèse vocale Google) depuis le Play Store, puis dans les paramètres Android › Accessibilité › Synthèse vocale, télécharge la voix **anglaise**.

## Méthode alternative : Android Studio

1. Télécharge et installe **Android Studio** : <https://developer.android.com/studio> (garde les options par défaut, il installe le SDK Android tout seul).
2. **File › Open** et choisis le dossier `EnglishQuest` (celui qui contient `settings.gradle.kts`).
3. Attends la fin de la « Gradle sync » (barre en bas, quelques minutes la première fois).
4. Menu **Build › Build App Bundle(s) / APK(s) › Build APK(s)**. Une bulle apparaît : clique sur **locate** pour trouver `app-debug.apk` (dans `app/build/outputs/apk/debug/`).
5. Ou bien branche ton téléphone en USB avec le **débogage USB** activé (Paramètres › À propos › appuie 7 fois sur « Numéro de build », puis Options pour les développeurs › Débogage USB) et clique sur le bouton ▶ **Run**.

## Ajouter du contenu

Tout le contenu est dans `app/src/main/assets/content.json` :

- **Un mot** = une ligne dans `"words"` :
  ```json
  {"id": "w600", "en": "to thrive", "fr": "s'épanouir", "theme": "identity", "example": "Plants thrive in the sun.", "note": ""}
  ```
  `id` unique, `theme` = un des thèmes existants. L'exemple doit contenir le mot (sans « to ») pour générer des textes à trous. Les mots d'un thème sont répartis automatiquement entre les unités de ce thème.
- **Un exercice de grammaire** dans `"items"` d'une leçon :
  - QCM : `{"type": "mcq", "prompt": "I ___ here.", "options": ["am", "is"], "answer": 0, "explain": "…"}`
  - Texte à trous : `{"type": "fill", "prompt": "She ___ (go) home.", "answers": ["went"], "explain": "…"}`
  - Remettre dans l'ordre / traduire : `{"type": "order", "en": "I have never seen it", "fr": "Je ne l'ai jamais vu", "explain": "…"}` (sans ponctuation dans `en`)
  - Repérer l'erreur : `{"type": "error", "sentence": "He don't know.", "wrong": 1, "correction": "doesn't", "explain": "…"}` (`wrong` = position du mot faux, en partant de 0)

## Architecture

```
app/src/main/java/com/englishquest/app/
├── MainActivity.kt, EnglishQuestApp.kt
├── data/      Content.kt (lecture du JSON), Progress.kt (modèle + JSON), ProgressStore.kt (DataStore)
├── srs/       Sm2.kt (répétition espacée)
├── game/      Exercise.kt, ExerciseFactory.kt, SessionBuilder.kt, Session.kt (moteur), AnswerCheck.kt, Gamification.kt
├── audio/     Speaker.kt (TextToSpeech), SoundFx.kt (sons synthétisés)
├── notif/     Reminders.kt (WorkManager + notification)
└── ui/        AppViewModel.kt, AppRoot.kt, theme/, components/, screens/
```

Kotlin 2.1 · Jetpack Compose (Material 3) · DataStore · WorkManager · minSdk 26 (Android 8.0) · targetSdk 35.
