import Box from "@mui/material/Box";
import Stack from "@mui/material/Stack";
import Typography from "@mui/material/Typography";
import type { ReactNode } from "react";

export function PageHeading({
  eyebrow,
  title,
  description,
}: {
  eyebrow: string;
  title: string;
  description?: string;
}) {
  return (
    <Box className="hero" component="section">
      <Box className="hero-inner">
        <Typography className="eyebrow" component="p" variant="overline">{eyebrow}</Typography>
        <Typography component="h1" variant="h1">{title}</Typography>
        {description && <Typography component="p" variant="body1">{description}</Typography>}
      </Box>
    </Box>
  );
}

export function PanelHeading({
  eyebrow,
  title,
  trailing,
}: {
  eyebrow?: string;
  title: string;
  trailing?: ReactNode;
}) {
  return (
    <Stack className="panel-heading" direction="row" sx={{ alignItems: "center", justifyContent: "space-between", gap: 2 }}>
      <Box>
        {eyebrow && <Typography className="eyebrow" component="p" variant="overline">{eyebrow}</Typography>}
        <Typography component="h2" variant="h6">{title}</Typography>
      </Box>
      {trailing}
    </Stack>
  );
}
