# IntelliJ IDEA Quick Start Guide

## Running with Different Profiles in IntelliJ IDEA

### Quick Answer
Add this to **Program arguments** in your run configuration:
```
--spring.profiles.active=prod
```

---

## Step-by-Step Instructions

### 1. Open Run Configuration
- Click **Run** → **Edit Configurations...** (or click the dropdown next to the Run button)

### 2. Select Your Application
- Find your Spring Boot application (usually named `ClientApplication` or similar)

### 3. Add Profile Setting (Choose ONE method)

#### Method 1: Program Arguments ✅ (Recommended)
**Field**: "Program arguments"  
**Value**: `--spring.profiles.active=prod`

**Example**:
```
Program arguments: --spring.profiles.active=prod
```

For sandbox (if you want to be explicit):
```
Program arguments: --spring.profiles.active=sandbox
```

---

#### Method 2: VM Options
**Field**: "VM options"  
**Value**: `-Dspring.profiles.active=prod`

**Example**:
```
VM options: -Dspring.profiles.active=prod
```

---

#### Method 3: Environment Variables
**Field**: "Environment variables"  
**Value**: `SPRING_PROFILES_ACTIVE=prod`

**Example**:
```
Environment variables: SPRING_PROFILES_ACTIVE=prod
```

---

#### Method 4: Active Profiles (If Available)
Some IntelliJ versions have a dedicated "Active profiles" field:
**Field**: "Active profiles"  
**Value**: `prod`

---

### 4. Apply and Run
- Click **Apply**
- Click **OK**
- Run your application normally (green play button)

---

## Creating Multiple Run Configurations

You can create separate run configurations for each environment:

1. **Duplicate your configuration**:
   - Right-click on your configuration → **Copy Configuration**
   
2. **Name them appropriately**:
   - `ClientApplication (Sandbox)`
   - `ClientApplication (Prod)`
   
3. **Set different profiles for each**:
   - Sandbox: `--spring.profiles.active=sandbox`
   - Prod: `--spring.profiles.active=prod`

Now you can quickly switch between environments using the run configuration dropdown!

---

## Verifying Active Profile

Check your console output when the application starts. You should see:
```
The following 1 profile is active: "prod"
```

Or for sandbox:
```
The following 1 profile is active: "sandbox"
```

---

## Troubleshooting

### Profile Not Loading?
1. Make sure there are no typos in the profile name
2. Check that `application-prod.properties` exists in `src/main/resources/`
3. Try cleaning and rebuilding: **Build** → **Rebuild Project**

### Wrong Collection IDs Being Used?
- Verify which profile is active in the console output
- Check that the correct properties file is being loaded
- Ensure you've restarted the application after changing the profile

---

## Quick Reference

| Environment | Program Argument |
|-------------|------------------|
| Sandbox (default) | *(not needed)* or `--spring.profiles.active=sandbox` |
| Production | `--spring.profiles.active=prod` |

**Collection IDs Used**:
- **Sandbox**: 451, 452, 453, 454, 455, 456
- **Production**: 405, 408, 409, 410, 411, 412

