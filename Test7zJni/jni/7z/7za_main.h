

#ifndef _7ZA_H_
#define _7ZA_H_

extern int main(int numArgs, const char *argv[]);

#ifdef __cplusplus
extern "C"{
#endif

    // Port 7zip main entry.
    int run_7za(int argc , const char *argv[]);
    
#ifdef __cplusplus
}
#endif

#endif  // _7ZA_H_
