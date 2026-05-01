-- Migration: Alter photo column to support larger values (TEXT instead of VARCHAR(255))
-- This fixes the error: "valeur trop longue pour le type character varying(255)"
-- Run this script in PostgreSQL to fix the column type

ALTER TABLE utilisateurs ALTER COLUMN photo TYPE TEXT;
