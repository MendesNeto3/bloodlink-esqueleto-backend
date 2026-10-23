package com.bloodlink.auth.usuario.application.in.web;

public record AtivarContaCommand(String token, String senha) {
    @Override
    public String toString() {
        return "AtivarContaCommand[***]";
    }
}