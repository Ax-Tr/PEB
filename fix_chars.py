import os
import re

backend_dir = r'c:\Users\Axiora User-36\Desktop\New folder\PEB\backend'

for root, dirs, files in os.walk(backend_dir):
    for file in files:
        if file.endswith('.java'):
            filepath = os.path.join(root, file)
            with open(filepath, 'r', encoding='utf-8') as f:
                content = f.read()
            
            # Find any @Column that has length = 26 or length = 10 or length = 2 or length = 3 that is meant to be char
            # Let's specifically target length = 26 for now, as that's ULID/KSUID.
            if '@Column(length = 26)' in content or '@Column(name =' in content or '@Column(nullable' in content:
                # We want to replace @Column(length = 26) with @JdbcTypeCode(SqlTypes.CHAR)\n  @Column(length = 26, columnDefinition = "char(26)")
                # But we need to use regex to catch all variations.
                # Actually, easier: replace all @Column(length = 26) with @JdbcTypeCode(SqlTypes.CHAR)\n  @Column(length = 26, columnDefinition = "char(26)")
                # And for @Column(name = "...", length = 26):
                # @JdbcTypeCode(SqlTypes.CHAR)\n  @Column(name = "...", length = 26, columnDefinition = "char(26)")
                
                original_content = content
                
                # Replace length = 26
                content = re.sub(r'@Column\(length = 26\)', r'@JdbcTypeCode(SqlTypes.CHAR)\n  @Column(length = 26, columnDefinition = "char(26)")', content)
                content = re.sub(r'@Column\(name = "([^"]+)", length = 26\)', r'@JdbcTypeCode(SqlTypes.CHAR)\n  @Column(name = "\1", length = 26, columnDefinition = "char(26)")', content)
                content = re.sub(r'@Column\(name = "([^"]+)", length = 26, nullable = (true|false)\)', r'@JdbcTypeCode(SqlTypes.CHAR)\n  @Column(name = "\1", length = 26, nullable = \2, columnDefinition = "char(26)")', content)

                if content != original_content:
                    # Add imports if not present
                    if 'import org.hibernate.annotations.JdbcTypeCode;' not in content:
                        # insert after the last import
                        import_stmt = "import org.hibernate.annotations.JdbcTypeCode;\nimport org.hibernate.type.SqlTypes;\n"
                        content = re.sub(r'(import .*;\n)(?!import )', r'\1' + import_stmt, content, count=1)
                        
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(content)
                    print(f"Updated {file}")
