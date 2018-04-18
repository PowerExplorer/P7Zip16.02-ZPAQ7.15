///*
// * Copyright 2015 Google, Inc.
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *   http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//
//package com.google.classysharkandroid.reflector;
//
//import java.lang.reflect.*;
//import java.util.*;
//
//public class Reflector {
//
//    private Class clazz;
//    private List<TaggedWord> words;
//
//    public enum TAG {
//        MODIFIER, IDENTIFIER, DOCUMENT
//    }
//
//    public static class TaggedWord {
//
//        public TaggedWord(String word, TAG tag) {
//            this.text = word;
//            this.tag = tag;
//        }
//
//        public String text;
//        public TAG tag;
//    }
//
//    public Reflector(Class clazz) {
//        this.clazz = clazz;
//        words = new ArrayList<>();
//    }
//
//    public String toString() {
//        if (words == null) {
//            return "";
//        } else {
//            StringBuilder sb = new StringBuilder();
//
//            for (TaggedWord word : words) {
//                sb.append(word.text);
//            }
//
//            return sb.toString();
//        }
//    }
//
//    public List<TaggedWord> getWords() {
//        return this.words;
//    }
//
//    public void generateClassData() {
//
//        long start = System.currentTimeMillis();
//
//        Constructor constructors[];
//        Class cc[];
//        Method methods[];
//        Field fields[];
//        Class currentClass = null;
//        Class supClass;
//        String x, y;
//        Hashtable classRef;
//
//        currentClass = clazz;
//
//        /*
//         * Step 0: If our name contains dots we're in a package so put
//         * that out first.
//         */
//        x = currentClass.getName();
//        if (x.lastIndexOf(".") != -1) {
//            y = x.substring(0, x.lastIndexOf("."));
//
//            words.add(new TaggedWord("\npackage ", TAG.MODIFIER));
//            words.add(new TaggedWord(y, TAG.IDENTIFIER));
//            words.add(new TaggedWord(";", TAG.MODIFIER));
//        }
//
//        fields = currentClass.getDeclaredFields();
//        constructors = currentClass.getDeclaredConstructors();
//        methods = currentClass.getDeclaredMethods();
//
//        classRef = generateDependencies(constructors, methods, fields);
//
//        // Don't import ourselves ...
//        classRef.remove(currentClass.getName());
//        fillTaggedText(constructors, methods, fields, currentClass, classRef);
//
//        long finish = System.currentTimeMillis();
//
//        System.out.println("* " + (finish - start) + "ms");
//    }
//
//    private Hashtable generateDependencies(Constructor[] constructors, Method[] methods, Field[] fields) {
//
//        String x, y;
//        Hashtable classRef = new Hashtable();
//
//        for (int i = 0; i < fields.length; i++) {
//            x = ClassTypeAlgorithm.TypeName(fields[i].getType().getName(), classRef);
//        }
//
//        for (int i = 0; i < constructors.length; i++) {
//            Class cx[] = constructors[i].getParameterTypes();
//            if (cx.length > 0) {
//                for (int j = 0; j < cx.length; j++) {
//                    x = ClassTypeAlgorithm.TypeName(cx[j].getName(), classRef);
//                }
//            }
//        }
//
//        for (int i = 0; i < methods.length; i++) {
//            x = ClassTypeAlgorithm.TypeName(methods[i].getReturnType().getName(), classRef);
//            Class cx[] = methods[i].getParameterTypes();
//            if (cx.length > 0) {
//                for (int j = 0; j < cx.length; j++) {
//                    x = ClassTypeAlgorithm.TypeName(cx[j].getName(), classRef);
//                }
//            }
//
//            Class<?>[] xType = methods[i].getExceptionTypes();
//
//            for (int j = 0; j < xType.length; j++) {
//                x = ClassTypeAlgorithm.TypeName(xType[j].getName(), classRef);
//            }
//        }
//        return classRef;
//    }
//
//    private void fillTaggedText(Constructor[] constructors, Method[] methods, Field[] fields, Class currentClass, Hashtable classRef) {
//
//        Class supClass;
//        String x;
//
//        for (Enumeration e = classRef.keys(); e.hasMoreElements(); ) {
//            Object importIdentifier = e.nextElement();
//            words.add(new TaggedWord("\nimport ", TAG.MODIFIER));
//            words.add(new TaggedWord(importIdentifier + ";", TAG.IDENTIFIER));
//        }
//        words.add(new TaggedWord("\n\n", TAG.IDENTIFIER));
//
//        int mod = currentClass.getModifiers();
//        words.add(new TaggedWord(Modifier.toString(mod), TAG.MODIFIER));
//
//        if (!Modifier.isInterface(mod)) {
//            words.add(new TaggedWord(" class", TAG.MODIFIER));
//        }
//        words.add(new TaggedWord(" " + ClassTypeAlgorithm.TypeName(currentClass.getName(), null), TAG.IDENTIFIER));
//
//        supClass = currentClass.getSuperclass();
//        if (supClass != null) {
//            words.add(new TaggedWord(" extends ", TAG.MODIFIER));
//            words.add(new TaggedWord(ClassTypeAlgorithm.TypeName(supClass.getName(), classRef), TAG.IDENTIFIER));
//        }
//        words.add(new TaggedWord("\n{", TAG.IDENTIFIER));
//
//        words.add(new TaggedWord("\n" +
//                "/*\n" +
//                " * Field Definitions.\n" +
//                " */", TAG.DOCUMENT));
//
//        for (int i = 0; i < fields.length; i++) {
//            Class ctmp = fields[i].getType();
//            int md = fields[i].getModifiers();
//
//            words.add(new TaggedWord("\n      " + Modifier.toString(md) + " ", TAG.MODIFIER));
//            words.add(new TaggedWord(ClassTypeAlgorithm.TypeName(fields[i].getType().getName(), null) + " ",
//                    TAG.IDENTIFIER));
//            words.add(new TaggedWord(fields[i].getName() + ";", TAG.DOCUMENT));
//        }
//
//        // TODO ENUMS members
//        // http://stackoverflow.com/questions/140537/how-to-use-java-reflection-when-the-enum-type-is-a-class
//
//        words.add(new TaggedWord("\n" +
//                "/*\n" +
//                " * Declared Constructors.\n" +
//                " */\n", TAG.DOCUMENT));
//        x = ClassTypeAlgorithm.TypeName(currentClass.getName(), null);
//        for (int i = 0; i < constructors.length; i++) {
//            int md = constructors[i].getModifiers();
//            words.add(new TaggedWord("    " + Modifier.toString(md) + " ", TAG.MODIFIER));
//            words.add(new TaggedWord(x, TAG.IDENTIFIER));
//
//            Class cx[] = constructors[i].getParameterTypes();
//            words.add(new TaggedWord("(", TAG.IDENTIFIER));
//            if (cx.length > 0) {
//                for (int j = 0; j < cx.length; j++) {
//                    words.add(new TaggedWord(ClassTypeAlgorithm.TypeName(cx[j].getName(), null), TAG.IDENTIFIER));
//                    if (j < (cx.length - 1)) {
//                        words.add(new TaggedWord(", ", TAG.IDENTIFIER));
//                    }
//                }
//            }
//
//            words.add(new TaggedWord(") { ... }\n", TAG.IDENTIFIER));
//        }
//
//        for (int i = 0; i < methods.length; i++) {
//            int md = methods[i].getModifiers();
//
//            words.add(new TaggedWord("    " + Modifier.toString(md) + " ", TAG.MODIFIER));
//            words.add(new TaggedWord(ClassTypeAlgorithm.TypeName(methods[i].getReturnType().getName(), null) + " ",
//                    TAG.IDENTIFIER));
//            words.add(new TaggedWord(methods[i].getName(), TAG.DOCUMENT));
//
//            Class cx[] = methods[i].getParameterTypes();
//            words.add(new TaggedWord("(", TAG.IDENTIFIER));
//            if (cx.length > 0) {
//                for (int j = 0; j < cx.length; j++) {
//                    words.add(new TaggedWord(ClassTypeAlgorithm.TypeName(cx[j].getName(), classRef), TAG.IDENTIFIER));
//                    if (j < (cx.length - 1)) {
//                        words.add(new TaggedWord(", ", TAG.IDENTIFIER));
//                    }
//                }
//            }
//
//            words.add(new TaggedWord(") ", TAG.IDENTIFIER));
//
//            // TODO put to dependencies & imports
//            Class<?>[] xType = methods[i].getExceptionTypes();
//
//            if (xType.length > 0) {
//                words.add(new TaggedWord(" throws ", TAG.IDENTIFIER));
//            }
//
//            for (int j = 0; j < xType.length; j++) {
//                words.add(new TaggedWord(xType[j].getSimpleName(), TAG.IDENTIFIER));
//            }
//
//            words.add(new TaggedWord("{ ... }\n", TAG.IDENTIFIER));
//        }
//
//        words.add(new TaggedWord("\n} ", TAG.IDENTIFIER));
//    }
//
//    public static void main(String[] args) {
//
//        Reflector reflector = new Reflector(Integer.class);
//        reflector.generateClassData();
//
//        System.out.print(reflector);
//    }
//}
