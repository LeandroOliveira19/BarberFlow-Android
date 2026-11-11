// Ficheiro de build de topo: os plugins são declarados aqui e aplicados no módulo :app.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.ksp) apply false
}
